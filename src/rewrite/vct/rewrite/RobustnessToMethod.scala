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
    "Extract a compound statement into a new function with VerCors pointer contracts."
}

case class RobustnessToMethod[Pre <: Generation]() extends Rewriter[Pre] {
  private var selected: Option[ToMethodSites.Site[Pre]] = None
  /** pre var -> (post param, derefInBody) */
  private var lifting: Seq[(Variable[Pre], Variable[Post], Boolean)] = Nil
  private var heapLifting: Seq[(LocalHeapVariable[Pre], Variable[Post])] = Nil
  private var extracting: Boolean = false
  private var enclosingLoopCond: Option[Expr[Pre]] = None

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

  private def lifted(v: Variable[Pre]): Option[(Variable[Post], Boolean)] =
    lifting.collectFirst {
      case (pre, post, deref) if pre eq v => (post, deref)
    }

  private def isPointerLike(t: Type[_]): Boolean =
    t match {
      case _: TPointer[_] | _: TNonNullPointer[_] | _: TPointerArray[_] |
          _: TNonNullPointerArray[_] | _: TConstPointerArray[_] |
          _: TNonNullConstPointerArray[_] | _: TArray[_] =>
        true
      case _ =>
        false
    }

  /** Prefer `T*` over sized `T[n]` in extracted params so size names are not
    * required before the array parameter (dependent type / scope crash).
    */
  private def decayToPointer(t: Type[Pre]): Type[Post] =
    t match {
      case TArray(el) => TPointer(dispatch(el), None)(t.o)
      case TPointerArray(el, _, _) => TPointer(dispatch(el), None)(t.o)
      case TNonNullPointerArray(el, _, unique) =>
        TPointer(dispatch(el), unique)(t.o)
      case TConstPointerArray(el, _) =>
        TPointer(dispatch(el), None)(t.o)
      case TNonNullConstPointerArray(el, _) =>
        TNonNullPointer(dispatch(el), None)(t.o)
      case other => dispatch(other)
    }

  private def paramType(t: Type[Pre], byRef: Boolean): Type[Post] =
    if (byRef) TPointer(dispatch(t), None)(t.o)
    else if (isPointerLike(t)) decayToPointer(t)
    else dispatch(t)

  /** A single written local can be returned to the caller. This also covers
    * pointer locals, whose pointer value must change in the caller when the
    * extracted block assigns them.
    */
  private def useReturnValue(
      written: Set[Variable[Pre]],
      captured: Seq[Variable[Pre]],
  ): Boolean =
    written.size == 1 &&
      (written.exists(v => isPointerLike(v.t)) ||
        captured.exists(v => isPointerLike(v.t)))

  override def dispatch(e: Expr[Pre]): Expr[Post] =
    e match {
      case Local(Ref(v)) =>
        lifted(v) match {
          case Some((param, true)) =>
            implicit val o: Origin = e.o
            DerefPointer[Post](param.get)(PanicBlame(
              "to-method pointer is the address of a live local"
            ))
          case Some((param, false)) =>
            Local[Post](param.ref)(e.o)
          case None =>
            e.rewriteDefault()
        }
      case HeapLocal(Ref(v)) =>
        heapLifting.collectFirst {
          case (pre, param) if pre eq v => Local[Post](param.ref)(e.o)
        }.getOrElse(e.rewriteDefault())
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

  /** Assigned locals need `\pointer(&x)` when they are passed by reference.
    * For-loops are turned into while so init runs before the framed while,
    * matching the pattern that already works for `3_while_multip`.
    */
  private def frameLoop(loop: Loop[Pre]): Statement[Post] = {
    implicit val o: Origin = loop.o
    val siteStat = selected.map(_.target)
    val captured = siteStat.map(ToMethodSites.capturedVars(_)).getOrElse(Nil)
    val written = siteStat.map(assignedLocals(_)).getOrElse(Set.empty)
    // A single written value is returned when pointer values participate in
    // the extraction. This keeps pointer reassignment visible to the caller
    // without adding an address-of frame.
    val useReturn = useReturnValue(written, captured)
    val byRef =
      if (useReturn) Nil
      else captured.filter(v => written.exists(_ eq v))
    val extra = foldStar(byRef.map { v =>
      PermPointer[Post](
        AddrOf(Local[Post](succ(v)))(o),
        const(1),
        WritePerm(),
      )(o)
    })

    def framedWhile(
        cond: Expr[Pre],
        contract: LoopContract[Pre],
        body: Statement[Pre],
    ): Loop[Post] = {
      enclosingLoopCond = Some(cond)
      try {
        Loop[Post](
          Block[Post](Nil),
          dispatch(cond),
          Block[Post](Nil),
          contract match {
            case inv: LoopInvariant[Pre] =>
              LoopInvariant[Post](
                Star(extra, dispatch(inv.invariant)),
                inv.decreases.map(dispatch),
              )(inv.o)
            case other =>
              dispatch(other)
          },
          dispatch(body),
        )
      } finally {
        enclosingLoopCond = None
      }
    }

    def hasSendRecv(node: Node[Pre]): Boolean =
      node match {
        case _: Send[Pre] | _: Recv[Pre] => true
        case _: Type[Pre] => false
        case other => other.subnodes.exists(hasSendRecv)
      }

    def keepAsFor(
        init: Statement[Pre],
        cond: Expr[Pre],
        update: Statement[Pre],
        contract: LoopContract[Pre],
        body: Statement[Pre],
    ): Loop[Post] = {
      enclosingLoopCond = Some(cond)
      try {
        Loop[Post](
          dispatch(init),
          dispatch(cond),
          dispatch(update),
          contract match {
            case inv: LoopInvariant[Pre] =>
              LoopInvariant[Post](
                Star(extra, dispatch(inv.invariant)),
                inv.decreases.map(dispatch),
              )(inv.o)
            case other =>
              dispatch(other)
          },
          dispatch(body),
        )
      } finally {
        enclosingLoopCond = None
      }
    }

    loop match {
      case Loop(Block(Nil), cond, Block(Nil), contract, body) =>
        framedWhile(cond, contract, body)
      // for→while only when we need AddrOf frames after init. Iteration
      // contracts / send-recv break if the for header is dismantled.
      case Loop(init, cond, update, contract, body)
          if byRef.nonEmpty && !hasSendRecv(loop) &&
            contract.isInstanceOf[LoopInvariant[Pre]] =>
        Block[Post](Seq(
          dispatch(init),
          framedWhile(
            cond,
            contract,
            Block[Pre](Seq(body, update))(o),
          ),
        ))
      case Loop(init, cond, update, contract, body)
          if hasSendRecv(loop) =>
        sendDecls.scope {
          keepAsFor(init, cond, update, contract, body)
        }
      case Loop(init, cond, update, contract, body) =>
        keepAsFor(init, cond, update, contract, body)
    }
  }

  private def freeLocals(e: Expr[Pre]): Seq[Variable[Pre]] =
    ToMethodSites.outwardVars(e)

  /** One assigned scalar + pointer captures: return the new value instead of
    * `T *` / AddrOf (avoids \\pointer(&local) next to array permissions).
    */
  private def extractReturn(
      stat: Statement[Pre],
      resultVar: Variable[Pre],
      fromBody: Seq[Variable[Pre]],
      fromGuard: Seq[Variable[Pre]],
      heapFromBody: Seq[LocalHeapVariable[Pre]],
      heapFromGuard: Seq[LocalHeapVariable[Pre]],
  ): Statement[Post] = {
    implicit val funcO: Origin = Origin(
      Seq(RequiredName("func_to_method"), LabelContext("to-method"))
    )
    val captured = fromBody ++ fromGuard
    val params = captured.map { v =>
      new Variable[Post](paramType(v.t, byRef = false))(v.o)
    }
    val capturedHeap = heapFromBody ++ heapFromGuard
    val heapParams = capturedHeap.map { v =>
      new Variable[Post](paramType(v.t, byRef = false))(v.o)
    }
    val resultParam = params(captured.indexWhere(_ eq resultVar))

    lifting = captured.zip(params).map { case (v, p) => (v, p, false) }
    heapLifting = capturedHeap.zip(heapParams)
    extracting = true
    val bodyCore = dispatch(stat)
    val guard = enclosingLoopCond.map(dispatch).getOrElse(tt[Post])
    extracting = false
    lifting = Nil
    heapLifting = Nil

    val body = Block[Post](Seq(
      bodyCore,
      Return[Post](Local[Post](resultParam.ref)(funcO))(funcO),
    ))(funcO)

    val extracted: Procedure[Post] = globalDeclarations.declare(
      procedure(
        blame = PanicBlame("to-method extracted function"),
        contractBlame = UnsafeDontCare.Satisfiability("to-method"),
        returnType = dispatch(resultVar.t),
        args = params ++ heapParams,
        body = Some(body),
        requires = UnitAccountedPredicate(guard)(funcO),
        ensures = UnitAccountedPredicate(tt[Post])(funcO),
        // Array perms stay with the caller; inline makes this sound.
        contextEverywhere = tt[Post],
        inline = true,
      )
    )

    val call = Assign[Post](
      Local[Post](succ(resultVar))(stat.o),
      procedureInvocation(
        blame = UnsafeDontCare.Invocation("to-method"),
        ref = new DirectRef[Post, Procedure[Post]](extracted),
        args = captured.map(v => Local[Post](succ(v))(stat.o)) ++
          capturedHeap.map(v => HeapLocal[Post](succ(v))(stat.o)),
      )(stat.o),
    )(PanicBlame("to-method assign result"))(stat.o)

    RobustnessStatementWrap.keepSurroundingBraces(stat, call)
  }

  private def extract(stat: Statement[Pre]): Statement[Post] = {
    implicit val funcO: Origin = Origin(
      Seq(RequiredName("func_to_method"), LabelContext("to-method"))
    )
    val fromBody = ToMethodSites.capturedVars(stat)
    val heapFromBody = ToMethodSites.outwardHeapVars(stat)
    val written = assignedLocals(stat)
    val fromGuard =
      enclosingLoopCond.map(freeLocals).getOrElse(Nil).filterNot(v =>
        fromBody.exists(_ eq v)
      )
    val heapFromGuard =
      enclosingLoopCond.map(ToMethodSites.outwardHeapVars).getOrElse(Nil)
        .filterNot(v => heapFromBody.exists(_ eq v))
    val useReturn = useReturnValue(written, fromBody)
    if (useReturn)
      return extractReturn(
        stat,
        written.head,
        fromBody,
        fromGuard,
        heapFromBody,
        heapFromGuard,
      )

    // Every assigned captured local is by reference unless the single-result
    // return path above is used. This includes pointer locals: assigning the
    // pointer value inside the extracted block must update the caller.
    val bodyModes: Seq[(Variable[Pre], Boolean /*byRef*/)] =
      fromBody.map { v =>
        val byRef = written.exists(_ eq v)
        (v, byRef)
      }
    val guardModes: Seq[(Variable[Pre], Boolean)] =
      fromGuard.map(v => (v, false))
    val capturedModes = bodyModes ++ guardModes

    val params = capturedModes.map { case (v, byRef) =>
      new Variable[Post](paramType(v.t, byRef))(v.o)
    }
    val capturedHeap = heapFromBody ++ heapFromGuard
    val heapParams = capturedHeap.map { v =>
      new Variable[Post](paramType(v.t, byRef = false))(v.o)
    }

    lifting = capturedModes.zip(params).map {
      case ((v, byRef), p) => (v, p, byRef)
    }
    heapLifting = capturedHeap.zip(heapParams)
    extracting = true
    val body = dispatch(stat)
    val guard = enclosingLoopCond.map(dispatch).getOrElse(tt[Post])
    extracting = false
    lifting = Nil
    heapLifting = Nil

    val byRefParams = capturedModes.zip(params).collect {
      case ((_, true), p) => p
    }
    val ptrValueParams = capturedModes.zip(params).collect {
      case ((v, false), p) if isPointerLike(v.t) => p
    } ++ heapParams
    val assignedRef = assignedByRefParams(body, byRefParams)
    val writesPtr = ptrValueParams.filter(p => writesThroughPointer(body, p))

    def permForRef(p: Variable[Post]): Expr[Post] = {
      val amount: Expr[Post] =
        if (assignedRef.exists(_ eq p)) WritePerm()(funcO)
        else ReadPerm()(funcO)
      PermPointer[Post](p.get, const(1), amount)(funcO)
    }
    def permForPtr(p: Variable[Post]): Expr[Post] = {
      val amount: Expr[Post] =
        if (writesPtr.exists(_ eq p)) WritePerm()(funcO)
        else ReadPerm()(funcO)
      PermPointer[Post](p.get, const(1), amount)(funcO)
    }

    val stmts = statementsOf(body)
    val assigns = stmts.flatMap(asPtrAssign(_, byRefParams))
    val straightLine =
      assigns.size == stmts.size &&
        assigns.map(_._1).distinct.size == assigns.size &&
        stmts.nonEmpty
    val contractSafe =
      straightLine && functionalContractSafe(assigns, byRefParams)
    val functional =
      if (contractSafe) functionalEnsures(body, byRefParams, assigns)(funcO)
      else tt[Post]
    // Array / iteration-contract contexts: inline and skip modular pointer
    // contracts (\\pointer(p, 1, _) is the wrong length and fights iter contracts).
    val arrayHeavy = ptrValueParams.nonEmpty
    val doInline = !contractSafe || arrayHeavy
    val contextPerms =
      if (arrayHeavy)
        tt[Post]
      else
        foldStar(
          byRefParams.map(permForRef) ++ ptrValueParams.map(permForPtr)
        )(funcO)

    val extracted: Procedure[Post] = globalDeclarations.declare(
      procedure(
        blame = PanicBlame("to-method extracted function"),
        contractBlame = UnsafeDontCare.Satisfiability("to-method"),
        returnType = TVoid(),
        args = params ++ heapParams,
        body = Some(body),
        requires = UnitAccountedPredicate(guard)(funcO),
        ensures = UnitAccountedPredicate(functional)(funcO),
        contextEverywhere = contextPerms,
        inline = doInline,
      )
    )

    val callArgs: Seq[Expr[Post]] = capturedModes.map {
      case (v, true) =>
        AddrOf(Local[Post](succ(v)))(stat.o)
      case (v, false) =>
        Local[Post](succ(v))(stat.o)
    } ++ capturedHeap.map(v => HeapLocal[Post](succ(v))(stat.o))

    val call = Eval[Post](
      procedureInvocation(
        blame = UnsafeDontCare.Invocation("to-method"),
        ref = new DirectRef[Post, Procedure[Post]](extracted),
        args = callArgs,
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

  private def writesThroughPointer(
      body: Statement[Post],
      p: Variable[Post],
  ): Boolean = {
    def isPtrBase(e: Expr[Post]): Boolean =
      e match {
        case Local(Ref(v)) => v eq p
        case DerefPointer(inner) => isPtrBase(inner)
        case _ => false
      }
    def go(node: Node[Post]): Boolean =
      node match {
        case Assign(target, _) =>
          target match {
            case AmbiguousSubscript(base, _) if isPtrBase(base) => true
            case PointerSubscript(base, _) if isPtrBase(base) => true
            case DerefPointer(Local(Ref(v))) if v eq p => true
            case _ => false
          }
        case Eval(PreAssignExpression(target, _)) =>
          target match {
            case AmbiguousSubscript(base, _) if isPtrBase(base) => true
            case PointerSubscript(base, _) if isPtrBase(base) => true
            case DerefPointer(Local(Ref(v))) if v eq p => true
            case _ => false
          }
        case _: Type[Post] => false
        case other => other.subnodes.exists(go)
      }
    go(body)
  }

  private def assignedByRefParams(
      body: Statement[Post],
      params: Seq[Variable[Post]],
  ): Seq[Variable[Post]] = {
    val out = scala.collection.mutable.LinkedHashSet.empty[Variable[Post]]
    def go(node: Node[Post]): Unit =
      node match {
        case Assign(DerefPointer(Local(Ref(p))), _)
            if params.exists(_ eq p) =>
          out += p
        case Eval(PreAssignExpression(DerefPointer(Local(Ref(p))), _))
            if params.exists(_ eq p) =>
          out += p
        case Eval(PostAssignExpression(DerefPointer(Local(Ref(p))), _))
            if params.exists(_ eq p) =>
          out += p
        case _: Type[Post] =>
        case other => other.subnodes.foreach(go)
      }
    go(body)
    out.toSeq
  }

  private def readsParam(
      node: Node[Post],
      param: Variable[Post],
  ): Boolean =
    node match {
      case DerefPointer(Local(Ref(p))) if p eq param => true
      case _: Type[Post] => false
      case other => other.subnodes.exists(readsParam(_, param))
    }

  /** functionalEnsures oldifies only the expression forms listed here. A
    * later assignment also cannot read a parameter changed by an earlier
    * assignment, because that read is no longer a pre-state value.
    */
  private def functionalContractSafe(
      assigns: Seq[(Variable[Post], Expr[Post])],
      params: Seq[Variable[Post]],
  ): Boolean = {
    def oldifySupported(e: Expr[Post]): Boolean =
      e match {
        case DerefPointer(Local(Ref(p))) if params.exists(_ eq p) => true
        case Plus(l, r) => oldifySupported(l) && oldifySupported(r)
        case Minus(l, r) => oldifySupported(l) && oldifySupported(r)
        case Mult(l, r) => oldifySupported(l) && oldifySupported(r)
        case AmbiguousPlus(l, r) =>
          oldifySupported(l) && oldifySupported(r)
        case AmbiguousMinus(l, r) =>
          oldifySupported(l) && oldifySupported(r)
        case AmbiguousMult(l, r) =>
          oldifySupported(l) && oldifySupported(r)
        case other => !params.exists(readsParam(other, _))
      }

    val assignedEarlier =
      scala.collection.mutable.LinkedHashSet.empty[Variable[Post]]
    assigns.forall { case (target, rhs) =>
      val safe =
        oldifySupported(rhs) &&
          !assignedEarlier.exists(readsParam(rhs, _))
      assignedEarlier += target
      safe
    }
  }

  private def functionalEnsures(
      body: Statement[Post],
      params: Seq[Variable[Post]],
      assigns: Seq[(Variable[Post], Expr[Post])],
  )(implicit o: Origin): Expr[Post] = {
    val oldBlame = PanicBlame("to-method \\old")
    val derefBlame = PanicBlame("to-method pointer is the address of a live local")
    val stmts = statementsOf(body)
    if (assigns.size != stmts.size || assigns.map(_._1).distinct.size != assigns.size)
      return tt

    val assigned = assigns.map(_._1)
    val fromAssigns = assigns.map { case (p, rhs) =>
      DerefPointer[Post](p.get)(derefBlame) ===
        oldifyParamReads(rhs, params)
    }
    val unchanged = params.filterNot(p => assigned.exists(_ eq p)).map { p =>
      val deref = DerefPointer[Post](p.get)(derefBlame)
      deref === Old(deref, None)(oldBlame)
    }
    foldStar(fromAssigns ++ unchanged)
  }

  private def oldifyParamReads(
      e: Expr[Post],
      params: Seq[Variable[Post]],
  )(implicit o: Origin): Expr[Post] = {
    val oldBlame = PanicBlame("to-method \\old")
    val derefBlame = PanicBlame("to-method pointer is the address of a live local")
    e match {
      case DerefPointer(Local(Ref(p))) if params.exists(_ eq p) =>
        Old(DerefPointer[Post](p.get)(derefBlame), None)(oldBlame)
      case Plus(l, r) =>
        Plus(oldifyParamReads(l, params), oldifyParamReads(r, params))
      case Minus(l, r) =>
        Minus(oldifyParamReads(l, params), oldifyParamReads(r, params))
      case Mult(l, r) =>
        Mult(oldifyParamReads(l, params), oldifyParamReads(r, params))
      case AmbiguousPlus(l, r) =>
        AmbiguousPlus(
          oldifyParamReads(l, params),
          oldifyParamReads(r, params),
        )(e.asInstanceOf[AmbiguousPlus[Post]].blame)
      case AmbiguousMinus(l, r) =>
        AmbiguousMinus(
          oldifyParamReads(l, params),
          oldifyParamReads(r, params),
        )(e.asInstanceOf[AmbiguousMinus[Post]].blame)
      case AmbiguousMult(l, r) =>
        AmbiguousMult(oldifyParamReads(l, params), oldifyParamReads(r, params))
      case other =>
        other
    }
  }

  private def assignedLocals(stat: Statement[Pre]): Set[Variable[Pre]] = {
    val out = scala.collection.mutable.LinkedHashSet.empty[Variable[Pre]]
    def go(node: Node[Pre]): Unit =
      node match {
        case Assign(Local(Ref(v)), _) => out += v
        case Eval(PreAssignExpression(Local(Ref(v)), _)) => out += v
        case Eval(PostAssignExpression(Local(Ref(v)), _)) => out += v
        case _: Type[Pre] =>
        case other => other.subnodes.foreach(go)
      }
    go(stat)
    out.toSet
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
