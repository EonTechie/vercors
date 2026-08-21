package vct.col.rewrite

import vct.col.ast._
import vct.col.origin.{Origin, PanicBlame}

trait CStatementPassBuilder {

  def key: String

  def desc: String

  def applyX[G <: Generation](): CStatementPass[G]
}

trait CStatementPass[G <: Generation] {

  def dispatch(
                program: Program[G]
              ): Program[G]
}

case object AddIfZero
  extends CStatementPassBuilder {

  override def key: String =
    "addIfZero"

  override def desc: String =
    "Wrap a C source statement in the else branch of if (0)."

  override def applyX[
    G <: Generation
  ](): CStatementPass[G] =
    AddIfZero[G]()
}

case class AddIfZero[
  G <: Generation
]() extends CStatementPass[G] {

  private var selected:
    Option[CStatementSites.Site[G]] =
    None

  private def cInt(
                    value: BigInt,
                    origin: Origin,
                  ): CIntegerValue[G] = {

    implicit val o: Origin =
      origin

    CIntegerValue[G](
      value,
      CPrimitiveType[G](
        Seq(
          CInt[G]()
        )
      )
    )
  }

  private def asSourceCompound(
                                stat: Statement[G],
                                origin: Origin,
                              ): Statement[G] =
    stat match {

      case scope @ Scope(
        Nil,
        Block(_)
      ) =>
        scope

      case other =>
        Scope[G](
          Nil,
          Block[G](
            Seq(other)
          )(origin)
        )(origin)
    }

  private def deadBranch(
                          origin: Origin
                        ): Statement[G] = {

    val assertion =
      Assert[G](
        BooleanValue[G](
          false
        )(origin)
      )(
        PanicBlame(
          "AddIfZero: dead branch became reachable"
        )
      )(origin)

    asSourceCompound(
      assertion,
      origin,
    )
  }

  private def transformedTarget(
                                 stat: Statement[G]
                               ): Statement[G] = {

    Branch[G](
      Seq(
        (
          cInt(
            BigInt(0),
            stat.o,
          ),
          deadBranch(stat.o),
        ),
        (
          BooleanValue[G](
            true
          )(stat.o),
          asSourceCompound(
            stat,
            stat.o,
          ),
        ),
      )
    )(stat.o)
  }

  private def rewriteStatement(
                                stat: Statement[G]
                              ): Statement[G] = {

    selected match {

      case Some(site)
        if site.target eq stat =>

        transformedTarget(stat)

      case _ =>

        stat match {

          case block @ Block(statements) =>

            val rewritten =
              statements.map(
                rewriteStatement
              )

            val changed =
              rewritten
                .zip(statements)
                .exists {
                  case (a, b) =>
                    !(a eq b)
                }

            if (changed)
              Block[G](
                rewritten
              )(block.o)
            else
              block

          case scope @ Scope(
            locals,
            body
          ) =>

            val rewrittenBody =
              rewriteStatement(body)

            if (rewrittenBody eq body)
              scope
            else
              Scope[G](
                locals,
                rewrittenBody,
              )(scope.o)

          case branch @ Branch(branches) =>

            val rewritten =
              branches.map {
                case (condition, body) =>
                  (
                    condition,
                    rewriteStatement(body),
                  )
              }

            val changed =
              rewritten
                .zip(branches)
                .exists {
                  case (
                    (_, newBody),
                    (_, oldBody),
                  ) =>
                    !(newBody eq oldBody)
                }

            if (changed)
              Branch[G](
                rewritten
              )(branch.o)
            else
              branch

          case loop @ Loop(
            init,
            cond,
            update,
            contract,
            body,
          ) =>

            /*
             * IMPORTANT:
             * We intentionally rewrite only the loop body.
             * for-init and for-update are not source
             * transformation sites for AddIfZero.
             */
            val rewrittenBody =
              rewriteStatement(body)

            if (rewrittenBody eq body)
              loop
            else
              Loop[G](
                init,
                cond,
                update,
                contract,
                rewrittenBody,
              )(loop.o)

          case label @ Label(
            decl,
            inner,
            contract,
          ) =>

            val rewrittenInner =
              rewriteStatement(inner)

            if (rewrittenInner eq inner)
              label
            else
              Label[G](
                decl,
                rewrittenInner,
                contract,
              )(label.o)

          case other =>
            other
        }
    }
  }

  private def rewriteGlobal(
                             decl: GlobalDeclaration[G]
                           ): GlobalDeclaration[G] =
    decl match {

      case unit: CTranslationUnit[G] =>

        val rewrittenDecls =
          unit.declarations.map(
            rewriteGlobal
          )

        val changed =
          rewrittenDecls
            .zip(unit.declarations)
            .exists {
              case (a, b) =>
                !(a eq b)
            }

        if (changed)
          new CTranslationUnit[G](
            rewrittenDecls
          )(unit.o)
        else
          unit

      case function:
        CFunctionDefinition[G] =>

        val rewrittenBody =
          rewriteStatement(
            function.body
          )

        if (
          rewrittenBody eq
            function.body
        ) {
          function
        } else {

          val rewrittenFunction =
            new CFunctionDefinition[G](
              function.contract,
              function.specs,
              function.declarator,
              rewrittenBody,
            )(
              function.blame
            )(
              function.o
            )

          rewrittenFunction.ref =
            function.ref

          rewrittenFunction
        }

      case other =>
        /*
         * struct, typedef, global variables etc.
         * are returned EXACTLY as they are.
         */
        other
    }

  override def dispatch(
                         program: Program[G]
                       ): Program[G] = {
    println("[DEBUG] CStatementSites.collect BASLIYOR")

    val candidates =
      CStatementSites.collect(
        program
      )

    println("[DEBUG] CStatementSites.collect BITTI")

    println(
      s"[AddIfZero] candidate count = ${candidates.size}"
    )

    candidates.zipWithIndex.foreach {
      case (site, index) =>
        println(
          s"[AddIfZero] candidate $index" +
            s" | function=${site.functionName}" +
            s" | role=${site.role.label}" +
            s" | path=${site.path}" +
            s" | kind=${site.description}"
        )
    }

    if (candidates.isEmpty) {
      selected = None
      return program
    }

    val selectedIndex =
      sys.env
        .get("ADD_IF_ZERO_SITE")
        .map(_.toInt)
        .getOrElse(
          throw new IllegalArgumentException(
            "ADD_IF_ZERO_SITE must be specified"
          )
        )

    if (
      selectedIndex < 0 ||
        selectedIndex >= candidates.size
    ) {
      throw new IllegalArgumentException(
        s"Invalid AddIfZero site index: $selectedIndex. " +
          s"Valid range: 0..${candidates.size - 1}"
      )
    }

    val site =
      candidates(
        selectedIndex
      )

    selected =
      Some(site)

    println(
      s"[AddIfZero] SELECTED INDEX = $selectedIndex"
    )

    println(
      s"[AddIfZero] SELECTED" +
        s" | function=${site.functionName}" +
        s" | role=${site.role.label}" +
        s" | path=${site.path}" +
        s" | kind=${site.description}"
    )

    val rewrittenDeclarations =
      program.declarations.map(
        rewriteGlobal
      )

    Program[G](
      rewrittenDeclarations
    )(
      program.blame
    )(
      program.o
    )
  }
}