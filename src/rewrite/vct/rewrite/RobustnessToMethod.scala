package vct.col.rewrite

import vct.col.ast._
import vct.col.origin.{
  LabelContext,
  Origin,
  PanicBlame,
  RequiredName,
  UnsafeDontCare,
}
import vct.col.ref.{DirectRef, Ref}
import vct.col.util.AstBuildHelpers._

case object RobustnessToMethod extends RewriterBuilder {
  override def key: String = "robustnessToMethod"

  override def desc: String =
    "Extract a compound statement into a new function, VerCors form: pointer args, \\pointer write, inline."
}

case class RobustnessToMethod[Pre <: Generation]() extends Rewriter[Pre] {
  private var selected: Option[ToMethodSites.Site[Pre]] = None
  private var lifting: Seq[(Variable[Pre], Variable[Post])] = Nil
  private var extracting: Boolean = false

  override def dispatch(program: Program[Pre]): Program[Post] = {
    val candidates = ToMethodSites.collect(program)

    println(s"[ToMethod] candidate count = ${candidates.size}")

    candidates.zipWithIndex.foreach { case (site, index) =>
      println(
        s"[ToMethod] candidate $index" +
          s" | function=${site.functionName}" +
          s" | path=${site.path}" +
          s" | kind=${site.description}"
      )
    }

    selected = RobustnessSiteSelection.nextIndex(
      "TO_METHOD_SITE",
      candidates.size,
      required = false,
    ).map { index =>
      val site = candidates(index)
      println(s"[ToMethod] SELECTED INDEX = $index")
      println(
        s"[ToMethod] SELECTED" +
          s" | function=${site.functionName}" +
          s" | path=${site.path}" +
          s" | kind=${site.description}"
      )
      site
    }

    super.dispatch(program)
  }

  private def lifted(v: Variable[Pre]): Option[Variable[Post]] =
    lifting.collectFirst { case (pre, post) if pre eq v => post }

  override def dispatch(e: Expr[Pre]): Expr[Post] =
    e match {
      case Local(Ref(v)) =>
        lifted(v) match {
          case Some(param) =>
            implicit val o: Origin = e.o
            DerefPointer[Post](param.get)(PanicBlame(
              "to-method pointer is the address of a live local"
            ))
          case None =>
            e.rewriteDefault()
        }
      case _ =>
        e.rewriteDefault()
    }

  override def dispatch(stat: Statement[Pre]): Statement[Post] =
    stat match {
      case loop: Loop[Pre]
          if selected.exists(site => containsStmt(loop, site.target)) =>
        frameLoop(loop)
      case s if !extracting && selected.exists(site => site.target eq s) =>
        extract(s)
      case _ =>
        stat.rewriteDefault()
    }

  private def containsStmt(node: Node[Pre], target: Statement[Pre]): Boolean =
    node match {
      case s: Statement[Pre] if s eq target => true
      case _: Type[Pre] => false
      case other => other.subnodes.exists(containsStmt(_, target))
    }

  /** AddrOf of a local makes Verify turn that local into a pointer. The
    * enclosing loop must frame `\pointer(&x, 1, write)` or the invariant
    * cannot mention `x`.
    */
  private def frameLoop(loop: Loop[Pre]): Loop[Post] = {
    implicit val o: Origin = loop.o
    val captured = selected.map(s => ToMethodSites.capturedVars(s.target))
      .getOrElse(Nil)
    val extra = foldStar(captured.map { v =>
      PermPointer[Post](
        AddrOf(Local[Post](succ(v)))(o),
        const(1),
        WritePerm(),
      )(o)
    })
    Loop[Post](
      dispatch(loop.init),
      dispatch(loop.cond),
      dispatch(loop.update),
      loop.contract match {
        case inv: LoopInvariant[Pre] =>
          LoopInvariant[Post](
            Star(extra, dispatch(inv.invariant)),
            inv.decreases.map(dispatch),
          )(inv.o)
        case other =>
          dispatch(other)
      },
      dispatch(loop.body),
    )
  }

  /** SemTransforms: `{ S }` using locals/params `x` becomes
    * `void f(T *x) { S[x := *x] }` and the site becomes `f(&x)`.
    * VerCors: `\pointer(x, 1, write)` on each arg, and `inline` so the
    * original loop contracts still prove after dump+verify.
    */
  private def extract(stat: Statement[Pre]): Statement[Post] = {
    implicit val funcO: Origin = Origin(
      Seq(RequiredName("func_to_method"), LabelContext("to-method"))
    )
    val captured = ToMethodSites.capturedVars(stat)
    val params = captured.map { v =>
      new Variable[Post](TPointer(dispatch(v.t), None)(v.o))(v.o)
    }

    lifting = captured.zip(params)
    extracting = true
    val body = dispatch(stat)
    extracting = false
    lifting = Nil

    def permStar: Expr[Post] =
      foldStar(params.map { p =>
        PermPointer[Post](p.get, const(1), WritePerm())(funcO)
      })(funcO)

    val extracted: Procedure[Post] = globalDeclarations.declare(
      procedure(
        blame = PanicBlame("to-method extracted function"),
        contractBlame = UnsafeDontCare.Satisfiability("to-method"),
        returnType = TVoid(),
        args = params,
        body = Some(body),
        requires = UnitAccountedPredicate(permStar)(funcO),
        ensures = UnitAccountedPredicate(
          Star(permStar, functionalEnsures(body, params)(funcO))(funcO)
        )(funcO),
        inline = false,
      )
    )

    val call = Eval[Post](
      procedureInvocation(
        blame = UnsafeDontCare.Invocation("to-method"),
        ref = new DirectRef[Post, Procedure[Post]](extracted),
        args = captured.map { v =>
          AddrOf(Local[Post](succ(v)))(stat.o)
        },
      )(stat.o)
    )(stat.o)

    RobustnessStatementWrap.keepSurroundingBraces(stat, call)
  }

  private def statementsOf(stat: Statement[Post]): Seq[Statement[Post]] =
    stat match {
      case Scope(_, inner) => statementsOf(inner)
      case Block(ss) => ss.flatMap(statementsOf)
      case other => Seq(other)
    }

  /** Straight-line `*p = e` becomes `ensures *p == \old(e)`, plus
    * `*q == \old(*q)` for pointer args that are not assigned. Matches the
    * VerCors-adapted to_method contracts on 3_while_multip.
    */
  private def functionalEnsures(
      body: Statement[Post],
      params: Seq[Variable[Post]],
  )(implicit o: Origin): Expr[Post] = {
    val oldBlame = PanicBlame("to-method \\old")
    val derefBlame = PanicBlame("to-method pointer is the address of a live local")
    val stmts = statementsOf(body)
    val assigns = stmts.flatMap(asPtrAssign(_, params))
    if (assigns.size != stmts.size || assigns.map(_._1).distinct.size != assigns.size)
      return tt

    val assigned = assigns.map(_._1)
    val fromAssigns = assigns.map { case (p, rhs) =>
      DerefPointer[Post](p.get)(derefBlame) ===
        Old(rhs, None)(oldBlame)
    }
    val unchanged = params.filterNot(p => assigned.exists(_ eq p)).map { p =>
      val deref = DerefPointer[Post](p.get)(derefBlame)
      deref === Old(deref, None)(oldBlame)
    }
    foldStar(fromAssigns ++ unchanged)
  }

  private def asPtrAssign(
      stat: Statement[Post],
      params: Seq[Variable[Post]],
  ): Option[(Variable[Post], Expr[Post])] = {
    def fromTarget(target: Expr[Post], rhs: Expr[Post])
        : Option[(Variable[Post], Expr[Post])] =
      target match {
        case DerefPointer(Local(Ref(p))) if params.exists(_ eq p) =>
          Some((p, rhs))
        case _ =>
          None
      }

    stat match {
      case Assign(target, rhs) =>
        fromTarget(target, rhs)
      case Eval(PreAssignExpression(target, value)) =>
        fromTarget(target, value)
      case Eval(PostAssignExpression(target, value)) =>
        fromTarget(target, value)
      case _ =>
        None
    }
  }
}
