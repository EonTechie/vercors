package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
import vct.col.ref.Ref
import vct.col.resolve.lang.C

object CInsertionSites {
  final case class Site[G](
      functionName: String,
      path: String,
      index: Int,
      container: Block[G],
      previous: Option[Statement[G]],
      next: Option[Statement[G]],
  ) {
    def description: String =
      s"InsertionSlot(index=$index, previous=${previous.map(describe).getOrElse("<start>")}, " +
        s"next=${next.map(describe).getOrElse("<end>")})"
  }

  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" || name == "abort" || name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  private def abruptControl[G](stat: Statement[G]): Boolean =
    stat match {
      case _: Return[G] | _: Throw[G] | _: Break[G] | _: Continue[G] |
          _: Goto[G] | _: CGoto[G] =>
        true
      case _ => false
    }

  private def splitDeclarationInitializer[G](
      previous: Statement[G],
      next: Statement[G],
  ): Boolean =
    (previous, next) match {
      case (LocalDecl(local), AssignInitial(Local(Ref(v)), _)) => v eq local
      case (LocalDecl(local), Assign(Local(Ref(v)), _)) => v eq local
      case _ => false
    }

  private def declarationCluster[G](stat: Statement[G]): Boolean =
    stat match {
      case _: CDeclarationStatement[G] | _: LocalDecl[G] | _: HeapLocalDecl[G] =>
        true

      case Scope(Nil, body) => declarationCluster(body)

      case Block(Seq(inner)) => declarationCluster(inner)

      case Block(Seq(LocalDecl(local), AssignInitial(Local(Ref(v)), _))) =>
        v eq local

      case Block(Seq(LocalDecl(local), Assign(Local(Ref(v)), _))) =>
        v eq local

      case _ => false
    }

  private def selectableSlot[G](
      statements: Seq[Statement[G]],
      index: Int,
  ): Boolean = {
    val previous = if (index == 0) None else Some(statements(index - 1))
    val next = if (index == statements.size) None else Some(statements(index))

    !previous.exists(abruptControl) &&
    !(previous.nonEmpty && next.nonEmpty &&
      splitDeclarationInitializer(previous.get, next.get))
  }

  private def describe[G](stat: Statement[G]): String =
    stat match {
      case Scope(locals, Block(stats)) =>
        s"CompoundStatement(locals=${locals.size}, items=${stats.size})"
      case Scope(_, Loop(init, cond, update, _, body)) =>
        s"LoopScope(init=${init.getClass.getSimpleName}, " +
          s"cond=${cond.getClass.getSimpleName}, " +
          s"update=${update.getClass.getSimpleName}, " +
          s"body=${body.getClass.getSimpleName})"
      case Loop(init, cond, update, _, body) =>
        s"LoopStatement(init=${init.getClass.getSimpleName}, " +
          s"cond=${cond.getClass.getSimpleName}, " +
          s"update=${update.getClass.getSimpleName}, " +
          s"body=${body.getClass.getSimpleName})"
      case Branch(branches) => s"IfStatement(arms=${branches.size})"
      case _: AssignInitial[G] => "InitialAssignmentStatement"
      case _: AssignStmt[G] => "AssignmentStatement"
      case Eval(expr) =>
        s"ExpressionStatement(expr=${expr.getClass.getSimpleName})"
      case _: InvocationStatement[G] => "InvocationStatement"
      case _: CGoto[G] | _: Goto[G] => "GotoStatement"
      case _: Break[G] => "BreakStatement"
      case _: Continue[G] => "ContinueStatement"
      case _: Return[G] => "ReturnStatement"
      case _: CDeclarationStatement[G] | _: LocalDecl[G] => "DeclarationStatement"
      case _: NonExecutableStatement[G] => "NonExecutableStatement"
      case other => other.getClass.getSimpleName
    }

  def collect[G](program: Program[G]): Seq[Site[G]] = {
    val result = ArrayBuffer.empty[Site[G]]

    def addBlockSlots(
        functionName: String,
        path: String,
        block: Block[G],
    ): Unit = {
      val statements = block.statements
      (0 to statements.size).foreach { index =>
        if (selectableSlot(statements, index)) {
          result += Site(
            functionName = functionName,
            path = s"$path.slot[$index]",
            index = index,
            container = block,
            previous = if (index == 0) None else Some(statements(index - 1)),
            next =
              if (index == statements.size)
                None
              else
                Some(statements(index)),
          )
        }
      }
    }

    def descend(functionName: String, path: String, stat: Statement[G]): Unit =
      stat match {
        case _ if declarationCluster(stat) =>

        case block @ Block(statements) =>
          addBlockSlots(functionName, path, block)
          statements.zipWithIndex.foreach { case (child, index) =>
            descend(functionName, s"$path.block[$index]", child)
          }

        case Scope(_, body) =>
          descend(functionName, s"$path.scope", body)

        case Branch(branches) =>
          branches.zipWithIndex.foreach { case ((_, body), index) =>
            descend(functionName, s"$path.branch[$index]", body)
          }

        case IndetBranch(branches) =>
          branches.zipWithIndex.foreach { case (body, index) =>
            descend(functionName, s"$path.indet[$index]", body)
          }

        case Loop(_, _, _, _, body) =>
          descend(functionName, s"$path.body", body)

        case Switch(_, body) =>
          descend(functionName, s"$path.switchBody", body)

        case Label(_, inner, _) =>
          descend(functionName, s"$path.labelBody", inner)

        case _ =>
      }

    def procedureName(procedure: Procedure[G]): String =
      procedure.o.getPreferredName.map(_.snake).getOrElse("<anonymous>")

    def visitGlobal(decl: GlobalDeclaration[G]): Unit =
      decl match {
        case unit: CTranslationUnit[G] => unit.declarations.foreach(visitGlobal)

        case function: CFunctionDefinition[G] =>
          val functionName = C.getDeclaratorInfo(function.declarator).name
          if (!blacklistedFunction(functionName)) {
            descend(functionName, s"$functionName.body", function.body)
          }

        case procedure: Procedure[G] =>
          val name = procedureName(procedure)
          if (!blacklistedFunction(name)) {
            procedure.body.foreach(descend(name, s"$name.body", _))
          }

        case _ =>
      }

    program.declarations.foreach(visitGlobal)
    result.toSeq
  }
}
