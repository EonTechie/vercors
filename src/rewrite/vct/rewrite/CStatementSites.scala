package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
import vct.col.resolve.lang.C

object CStatementSites {

  sealed trait Role {
    def label: String
  }

  final case class BlockItem(index: Int) extends Role {
    override def label: String =
      s"BLOCK_ITEM[$index]"
  }

  final case class BranchArm(index: Int) extends Role {
    override def label: String =
      if (index == 0)
        "IF_TRUE"
      else
        s"IF_ELSE[$index]"
  }

  case object LoopBody extends Role {
    override def label: String =
      "LOOP_BODY"
  }

  final case class Site[G](
                            functionName: String,
                            path: String,
                            role: Role,
                            target: Statement[G],
                          ) {

    def description: String =
      describe(target)
  }

  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" ||
      name == "abort" ||
      name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  /**
   * This is deliberately a whitelist.
   *
   * We do NOT say:
   *
   *     every Statement is selectable.
   *
   * We only accept shapes that CToCol produces for ordinary
   * executable C source statements.
   */
  private def selectable[G](
                             stat: Statement[G]
                           ): Boolean =
    stat match {

      // Supervisor requirement:
      // Return is never an AddIfOne/AddIfZero location.
      case _: Return[G] =>
        false

        // SemTransformers add_if1 explicitly excludes Decl.
      case _: CDeclarationStatement[G] =>
        false

        // VerCors specification/internal statements must not become
        // C source transformation locations.
      case _: NonExecutableStatement[G] =>
        false

        // C expression statement:
        // x = 1;
        // foo();
        // i++;
      case _: Eval[G] =>
        true

        // C if statement.
      case _: Branch[G] =>
        true

        // A parsed C compound statement:
        //
        // { ... }
        //
        // CToCol represents it exactly as Scope(Nil, Block(...)).
      case Scope(Nil, Block(_)) =>
        true

        // A parsed C while/for statement:
        //
        // CToCol wraps Loop in Scope.
      case Scope(
        Nil,
        Loop(_, _, _, _, _)
      ) =>
        true

        // Named C goto.
      case _: CGoto[G] =>
        true

        // These remain ordinary source statements.
      case _: Break[G] =>
        true

      case _: Continue[G] =>
        true

        /*
         * Intentionally no generic:
         *
         *   case _: Statement => true
         *
         * Anything not proven to represent a supported C source
         * statement is rejected.
         */
      case _ =>
        false
    }

  private def describe[G](
                           stat: Statement[G]
                         ): String =
    stat match {

      case Scope(Nil, Block(stats)) =>
        s"CompoundStatement(items=${stats.size})"

      case Scope(
        Nil,
        Loop(init, cond, update, _, body)
      ) =>
        s"LoopStatement(" +
          s"init=${init.getClass.getSimpleName}, " +
          s"cond=${cond.getClass.getSimpleName}, " +
          s"update=${update.getClass.getSimpleName}, " +
          s"body=${body.getClass.getSimpleName})"

      case Branch(branches) =>
        s"IfStatement(arms=${branches.size})"

      case Eval(expr) =>
        s"ExpressionStatement(expr=${expr.getClass.getSimpleName})"

      case _: CGoto[G] =>
        "GotoStatement"

      case _: Break[G] =>
        "BreakStatement"

      case _: Continue[G] =>
        "ContinueStatement"

      case _: Return[G] =>
        "ReturnStatement"

      case _: CDeclarationStatement[G] =>
        "DeclarationStatement"

      case other =>
        other.getClass.getSimpleName
    }

  def collect[G](
                  program: Program[G]
                ): Seq[Site[G]] = {

    val result =
      ArrayBuffer.empty[Site[G]]

    def add(
             functionName: String,
             path: String,
             role: Role,
             target: Statement[G],
           ): Unit = {

      if (selectable(target)) {
        result +=
          Site(
            functionName,
            path,
            role,
            target,
          )
      }
    }

    /*
     * Important:
     *
     * This traversal is NOT generic Node.subnodes traversal.
     *
     * It explicitly follows exactly the structural statement
     * positions corresponding to SemTransformers FindStatements.
     */
    def descend(
                 functionName: String,
                 path: String,
                 stat: Statement[G],
               ): Unit =
      stat match {

        /*
         * C compound statement:
         *
         * {
         *   statement0;
         *   statement1;
         * }
         *
         * Each direct block item is a possible location.
         */
        case Scope(
          Nil,
          Block(statements)
        ) =>

          statements.zipWithIndex.foreach {
            case (child, index) =>

              val childPath =
                s"$path.block[$index]"

              add(
                functionName,
                childPath,
                BlockItem(index),
                child,
              )

              descend(
                functionName,
                childPath,
                child,
              )
          }

          /*
           * CToCol's wrapper around while/for.
           *
           * The Scope itself represents the source loop statement.
           * The Loop node is an internal representation.
           */
        case Scope(
          Nil,
          loop @ Loop(_, _, _, _, _)
        ) =>

          descend(
            functionName,
            s"$path.loop",
            loop,
          )

          /*
           * IF:
           *
           * SemTransformers treats iftrue and iffalse as statement
           * locations.
           *
           * Conditions are NOT traversed as statement locations.
           */
        case Branch(branches) =>

          // First register the direct arms.
          // This mirrors FindStatements ordering.
          branches.zipWithIndex.foreach {
            case ((_, body), index) =>

              add(
                functionName,
                s"$path.branch[$index]",
                BranchArm(index),
                body,
              )
          }

          // Then inspect structures inside the arms.
          branches.zipWithIndex.foreach {
            case ((_, body), index) =>

              descend(
                functionName,
                s"$path.branch[$index]",
                body,
              )
          }

          /*
           * LOOP:
           *
           * ONLY body is a source-level statement location.
           *
           * init   -> deliberately ignored
           * cond   -> deliberately ignored
           * update -> deliberately ignored
           * contract -> deliberately ignored
           */
        case Loop(
          _,
          _,
          _,
          _,
          body
        ) =>

          val bodyPath =
            s"$path.body"

          add(
            functionName,
            bodyPath,
            LoopBody,
            body,
          )

          descend(
            functionName,
            bodyPath,
            body,
          )

          /*
           * Do not make Label.inner itself a location merely because
           * it is a Label child.
           *
           * But structures nested below it may still contain legal
           * sites.
           */
        case Label(_, inner, _) =>

          descend(
            functionName,
            s"$path.labelBody",
            inner,
          )

          /*
           * Eval, Return, declarations, Break, Continue, etc.
           * contain no further statement positions relevant here.
           *
           * Crucially: we do NOT recursively descend through arbitrary
           * Node.subnodes.
           */
        case _ =>
      }

    def visitGlobal(
                     decl: GlobalDeclaration[G]
                   ): Unit =
      decl match {

        case unit: CTranslationUnit[G] =>
          unit.declarations.foreach(visitGlobal)

        case function: CFunctionDefinition[G] =>

          val functionName =
            C
              .getDeclaratorInfo(
                function.declarator
              )
              .name

          if (!blacklistedFunction(functionName)) {

            /*
             * The function body itself is NOT a FindStatements
             * location.
             *
             * We enter it only to discover its contained
             * statement locations.
             */
            descend(
              functionName,
              s"$functionName.body",
              function.body,
            )
          }

        case _ =>
      }

    program.declarations.foreach(visitGlobal)

    result.toSeq
  }
}