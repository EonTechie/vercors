package vct.col.rewrite

import vct.col.ast._
import vct.col.origin.{
  LabelContext,
  Origin,
  PanicBlame,
  RequiredName,
  UnsafeDontCare,
}
import vct.col.resolve.ctx.{RefCFunctionDefinition, RefCGlobalDeclaration}
import vct.col.resolve.lang.C
import vct.col.util.AstBuildHelpers._

case object RobustnessDeepenWhile extends RewriterBuilder {
  override def key: String = "robustnessDeepenWhile"

  override def desc: String =
    "Nest one selected while as while (c) { while (nondet() && c) S }. " +
      "Outer keeps invariants and drops decreases; inner keeps both."
}

/** Existing `__VERIFIER_nondet_boolean` in the program, if any. A conflicting
  * signature means deepen-while does not run.
  */
object DeepenWhileNondet {
  val Name: String = "__VERIFIER_nondet_boolean"

  sealed trait Status[+G]
  case object Absent extends Status[Nothing]
  case class CompatibleProcedure[G](procedure: Procedure[G]) extends Status[G]
  case class CompatibleFunction[G](function: Function[G]) extends Status[G]
  case class CompatibleCFunction[G](function: CFunctionDefinition[G])
      extends Status[G]
  case class CompatibleCGlobal[G](decl: CGlobalDeclaration[G], initIndex: Int)
      extends Status[G]
  case object Conflict extends Status[Nothing]

  private def originName[G](decl: Declaration[G]): String =
    decl.o.getPreferredName.map(_.snake).getOrElse("")

  private def isBoolType[G](t: Type[G]): Boolean =
    t match {
      case TBool() => true
      case TConst(inner) => isBoolType(inner)
      case _ => false
    }

  private def cCompatible[G](
      specs: Seq[CDeclarationSpecifier[G]],
      declarator: CDeclarator[G],
  ): Boolean = {
    val info = C.getDeclaratorInfo(declarator)
    info.params.contains(Nil) && {
      try { isBoolType(C.typeOrReturnTypeFromDeclaration(specs, declarator)) }
      catch { case _: Throwable => false }
    }
  }

  private def procedureCompatible[G](procedure: Procedure[G]): Boolean =
    isBoolType(procedure.returnType) && procedure.args.isEmpty &&
      procedure.outArgs.isEmpty && procedure.typeArgs.isEmpty

  private def functionCompatible[G](function: Function[G]): Boolean =
    isBoolType(function.returnType) && function.args.isEmpty &&
      function.typeArgs.isEmpty

  def lookup[G](program: Program[G]): Status[G] = {
    var found: Status[G] = Absent

    def consider(compatible: Boolean, reuse: => Status[G]): Unit =
      if (!compatible)
        found = Conflict
      else if (found == Absent)
        found = reuse

    def visit(decl: GlobalDeclaration[G]): Unit =
      if (found != Conflict) {
        decl match {
          case unit: CTranslationUnit[G] =>
            unit.declarations.foreach(visit)

          case procedure: Procedure[G] if originName(procedure) == Name =>
            consider(procedureCompatible(procedure), CompatibleProcedure(procedure))

          case function: Function[G] if originName(function) == Name =>
            consider(functionCompatible(function), CompatibleFunction(function))

          case function: CFunctionDefinition[G]
              if C.getDeclaratorInfo(function.declarator).name == Name =>
            consider(
              cCompatible(function.specs, function.declarator),
              CompatibleCFunction(function),
            )

          case global: CGlobalDeclaration[G] =>
            global.decl.inits.zipWithIndex.foreach { case (init, index) =>
              if (found != Conflict && C.getDeclaratorInfo(init.decl).name == Name)
                consider(
                  cCompatible(global.decl.specs, init.decl),
                  CompatibleCGlobal(global, index),
                )
            }

          case _ =>
        }
      }

    program.declarations.foreach(visit)
    found
  }
}

case class RobustnessDeepenWhile[Pre <: Generation]() extends Rewriter[Pre] {
  private var selected: Option[DeepenWhileSites.Site[Pre]] = None
  private var nondetStatus: DeepenWhileNondet.Status[Pre] =
    DeepenWhileNondet.Absent

  private lazy val injectedNondet: Procedure[Post] = {
    implicit val o: Origin = Origin(
      Seq(
        RequiredName(DeepenWhileNondet.Name),
        LabelContext("deepen-while"),
      )
    )
    globalDeclarations.declare(
      procedure(
        blame = PanicBlame("deepen-while nondet is abstract"),
        contractBlame = UnsafeDontCare.Satisfiability("sv-comp nondet"),
        returnType = TBool(),
        body = None,
        pure = false,
      )
    )
  }

  override def dispatch(program: Program[Pre]): Program[Post] = {
    nondetStatus = DeepenWhileNondet.lookup(program)

    val candidates =
      nondetStatus match {
        case DeepenWhileNondet.Conflict =>
          println(
            "[DeepenWhile] skip: __VERIFIER_nondet_boolean already exists with an incompatible signature"
          )
          Seq.empty
        case _ =>
          DeepenWhileSites.collect(program)
      }

    println(s"[DeepenWhile] candidate count = ${candidates.size}")

    candidates.zipWithIndex.foreach { case (site, index) =>
      println(
        s"[DeepenWhile] candidate $index" +
          s" | function=${site.functionName}" +
          s" | path=${site.path}" +
          s" | kind=${site.description}"
      )
    }

    selected = RobustnessSiteSelection.nextIndex(
      "DEEPEN_WHILE_SITE",
      candidates.size,
      required = false,
    ).map { index =>
      val site = candidates(index)
      println(s"[DeepenWhile] SELECTED INDEX = $index")
      println(
        s"[DeepenWhile] SELECTED" +
          s" | function=${site.functionName}" +
          s" | path=${site.path}" +
          s" | kind=${site.description}"
      )
      site
    }

    super.dispatch(program)
  }

  override def dispatch(stat: Statement[Pre]): Statement[Post] =
    stat match {
      case loop: Loop[Pre] if selected.exists(site => site.target eq loop) =>
        deepen(loop)
      case _ => stat.rewriteDefault()
    }

  private def nondetCall(implicit o: Origin): Expr[Post] = {
    val blame = UnsafeDontCare.Invocation("sv-comp nondet")
    val panic = PanicBlame("deepen-while nondet call")
    nondetStatus match {
      case DeepenWhileNondet.CompatibleProcedure(procedure) =>
        procedureInvocation(blame = blame, ref = succ(procedure))
      case DeepenWhileNondet.CompatibleFunction(function) =>
        functionInvocation(blame = blame, ref = succ(function))
      case DeepenWhileNondet.CompatibleCFunction(function) =>
        val post = succ[CFunctionDefinition[Post]](function).decl
        val local = CLocal[Post](DeepenWhileNondet.Name)(panic)
        local.ref = Some(RefCFunctionDefinition(post))
        val inv = CInvocation[Post](local, Nil, Nil, Nil, false)(panic)
        inv.ref = Some(RefCFunctionDefinition(post))
        inv
      case DeepenWhileNondet.CompatibleCGlobal(decl, initIndex) =>
        val post = succ[CGlobalDeclaration[Post]](decl).decl
        val local = CLocal[Post](DeepenWhileNondet.Name)(panic)
        local.ref = Some(RefCGlobalDeclaration(post, initIndex))
        val inv = CInvocation[Post](local, Nil, Nil, Nil, false)(panic)
        inv.ref = Some(RefCGlobalDeclaration(post, initIndex))
        inv
      case DeepenWhileNondet.Absent | DeepenWhileNondet.Conflict =>
        procedureInvocation(blame = blame, ref = injectedNondet.ref)
    }
  }

  private def innerCond(loop: Loop[Pre])(implicit o: Origin): Expr[Post] =
    And(nondetCall, dispatch(loop.cond))

  /** Outer: same invariants, no decreases.
    * Inner: same invariants, the original DecreasesClause unchanged.
    */
  private def contracts(
      loop: Loop[Pre]
  ): (LoopContract[Post], LoopContract[Post]) =
    loop.contract match {
      case inv @ LoopInvariant(invariant, decreases) =>
        val outer = LoopInvariant[Post](dispatch(invariant), None)(inv.blame)(
          inv.o
        )
        val inner = LoopInvariant[Post](
          dispatch(invariant),
          decreases.map(clause => dispatch(clause)),
        )(inv.blame)(inv.o)
        (outer, inner)
      case other =>
        val copied = dispatch(other)
        (copied, copied)
    }

  /** `while (c) S` → `while (c) { while (nondet() && c) S }`.
    * Only the inner adapter is marked generated-by-deepen-while.
    */
  private def deepen(loop: Loop[Pre]): Statement[Post] = {
    val generated: Origin =
      loop.o.withContent(LabelContext(DeepenWhileSites.GeneratedLabel))
    val (outerContract, innerContract) = contracts(loop)
    val inner = {
      implicit val o: Origin = generated
      Loop[Post](
        Block[Post](Nil),
        innerCond(loop),
        Block[Post](Nil),
        innerContract,
        RobustnessStatementWrap.asCompound(dispatch(loop.body), loop.o),
      )
    }
    implicit val o: Origin = loop.o
    Loop[Post](
      Block[Post](Nil),
      dispatch(loop.cond),
      Block[Post](Nil),
      outerContract,
      RobustnessStatementWrap.asCompound(inner, loop.o),
    )
  }
}
