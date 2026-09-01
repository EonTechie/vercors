# What changed after the VerCors fork

This is the detailed companion to [`ROBUSTNESS-FILES.md`](ROBUSTNESS-FILES.md).
It is not a list of recently touched files. It records what this branch
(`robustness-post-resolution`) changed relative to upstream VerCors
(`origin/dev`).

- **Added:** the path did not exist on `origin/dev`. The current source is
  given in full.
- **Removed:** the file or flag existed on `origin/dev` and is gone here.
  The source that used to be there is quoted, then the note that it is gone.
- **Modified:** the path already existed on `origin/dev`. Old fragment, new
  fragment, and what that piece did versus what it does now.

How to add a transform that does not exist yet:
[`ROBUSTNESS.md`](ROBUSTNESS.md).

## A. Added files

### `src/main/vct/main/modes/Robustness.scala`

The `--robustness` mode. Parse, resolve, run the builders from `ROBUSTNESS_TRANSFORM` / spin, print C to `robustness-transformed.c`. Does not run Silicon.

**Old:** this path did not exist.

**Now:**

```scala
package vct.main.modes

import com.typesafe.scalalogging.LazyLogging
import vct.col.origin.BlameCollector
import vct.col.print.Ctx
import vct.col.rewrite.{
  AddIfOne,
  AddIfZero,
  InitialGeneration,
  RewriterBuilder,
  RobustnessDeepenWhile,
  RobustnessForLoopToWhileLoop,
  RobustnessToMethod,
  RobustnessSiteSelection,
  RobustnessSpin,
  RobustnessSpinStep,
}
import vct.main.Main.{EXIT_CODE_ERROR, EXIT_CODE_SUCCESS}
import vct.main.stages.{Output, Parsing, Resolution, Transformation}
import vct.options.Options
import vct.parsers.transform.ConstantBlameProvider
import vct.result.VerificationError.{SystemError, UserError}

import java.nio.file.{Path, Paths}
import scala.util.Random

case object Robustness extends LazyLogging {
  private val outputPath: Path = Paths.get("robustness-transformed.c")

  private def parseTransform(name: String): RewriterBuilder =
    name.trim.toLowerCase match {
      case "" | "add-if-zero" | "add_if_zero" =>
        AddIfZero

      case "add-if-one" | "add_if_one" =>
        AddIfOne

      case "for-to-while" | "for_to_while" =>
        RobustnessForLoopToWhileLoop

      case "deepen-while" | "deepen_while" =>
        RobustnessDeepenWhile

      case "to-method" | "to_method" =>
        RobustnessToMethod

      case other =>
        throw new IllegalArgumentException(
          s"Unknown ROBUSTNESS_TRANSFORM=$other. " +
            "Use add-if-zero, add-if-one, for-to-while, deepen-while, or to-method, " +
            "optionally comma-separated."
        )
    }

  private def envFlag(name: String): Boolean =
    sys.env.get(name).map(_.trim).exists { value =>
      val lower = value.toLowerCase
      lower == "1" || lower == "true" || lower == "yes"
    }

  private def envNames: Seq[String] = {
    val raw = sys.env.get("ROBUSTNESS_TRANSFORM").map(_.trim).filter(_.nonEmpty)
      .getOrElse("")
    raw.split("[,\\s]+").iterator.map(_.trim.toLowerCase).filter(_.nonEmpty)
      .toSeq
  }

  private def selectedBuilders(spin: Boolean): Seq[RewriterBuilder] = {
    val names = envNames
    if (names.isEmpty) {
      if (spin)
        Seq(
          AddIfZero,
          AddIfOne,
          RobustnessForLoopToWhileLoop,
          RobustnessDeepenWhile,
        )
      else
        Seq(AddIfZero)
    } else {
      names.map(parseTransform)
    }
  }

  private def spinEnabled(options: Options): Boolean =
    options.robustnessSpin || envFlag("ROBUSTNESS_SPIN")

  private def spinSeed(options: Options): Long =
    options.robustnessSeed.orElse {
      sys.env.get("ROBUSTNESS_SEED").map(_.trim).filter(_.nonEmpty).map { raw =>
        try { raw.toLong }
        catch {
          case _: NumberFormatException =>
            throw new IllegalArgumentException(
              s"ROBUSTNESS_SEED must be a Long, got: $raw"
            )
        }
      }
    }.getOrElse(Random.nextLong())

  def runOptions(options: Options): Int = {
    logger.info("Robustness mode started")

    val collector = BlameCollector()
    val blameProvider = ConstantBlameProvider(collector)
    val spin = spinEnabled(options)
    val builders = selectedBuilders(spin)
    val repeat = math.max(1, options.robustnessRepeat)

    RobustnessSiteSelection.reset()

    val passes =
      if (spin) {
        val seed = spinSeed(options)
        RobustnessSpin.reset(seed, builders.map(RobustnessSpin.kindOf))
        logger.info("Robustness mode: spin (random kind + random site)")
        logger.info(s"Robustness spin seed: $seed")
        logger.info(
          s"Robustness spin pool: ${builders.map(_.key).mkString(",")}"
        )
        Seq.fill(repeat)(RobustnessSpinStep)
      } else {
        // Same-kind (or cycling) N-fold apply on the resolved AST, site 0
        // unless the matching *_SITE env var is set.
        logger.info("Robustness mode: sequential")
        Seq.tabulate(repeat)(i => builders(i % builders.size))
      }

    logger.info(s"Robustness transforms: ${builders.map(_.key).mkString(",")}")
    logger.info(s"Robustness repeat: $repeat")

    // Parse and resolve once, then apply the selected robustness rewrite(s)
    // `repeat` times so later applications see the already-mutated AST.
    // Generated sizeof_* helpers stay in the resolved AST for pointer-size
    // reasoning, but C layout omits them (see Function.isGeneratedSizeOfHelper).
    // Inter-pass typecheck is skipped for repeat > 1: 100 nested wraps would
    // otherwise re-check the same well-formedness 100 times.
    val stages = Parsing.ofOptions[InitialGeneration](options, blameProvider)
      .thenRun(Resolution.ofOptions[InitialGeneration](options, blameProvider))
      .thenRun(new Transformation(
        onPassEvent = Nil,
        passes = passes,
        optimizeUnsafe = options.devUnsafeOptimization || repeat > 1,
      )).thenRun(Output(
        out = Some(outputPath),
        syntax = Ctx.C,
        splitDecls = false,
      ))

    stages.run(options.inputs) match {
      case Left(err: UserError) =>
        logger.error(err.text)
        EXIT_CODE_ERROR

      case Left(err: SystemError) => throw err

      case Right(_) =>
        if (spin) {
          val trace = RobustnessSpin.trace
          if (trace.isEmpty)
            logger.info("Robustness spin trace: (empty)")
          else
            logger.info("Robustness spin trace:\n" + trace.mkString("\n"))
        }
        logger.info("Robustness transformation completed")
        logger.info("Output written to robustness-transformed.c")
        EXIT_CODE_SUCCESS
    }
  }
}
```

### `src/rewrite/vct/rewrite/AddIfZero.scala`

Rewrite `add-if-zero`: wrap one collected statement as `if (0) { assert false } else { S }`.

**Old:** this path did not exist.

**Now:**

```scala
package vct.col.rewrite

import vct.col.ast._
import vct.col.ast.RewriteHelpers._
import vct.col.origin.{Origin, PanicBlame}

case object AddIfZero extends RewriterBuilder {
  override def key: String = "addIfZero"

  override def desc: String =
    "Wrap a C statement as if (0) { assert false } else { original }."
}

case class AddIfZero[Pre <: Generation]() extends Rewriter[Pre] {
  private var selected: Option[CStatementSites.Site[Pre]] = None
  private var wrapped: Boolean = false

  private def bool(value: Boolean, origin: Origin): BooleanValue[Post] =
    BooleanValue[Post](value)(origin)

  private def asCompound(
      stat: Statement[Post],
      origin: Origin,
  ): Statement[Post] = RobustnessStatementWrap.asCompound(stat, origin)

  private def deadBranch(origin: Origin): Statement[Post] =
    asCompound(
      Assert[Post](BooleanValue[Post](false)(origin))(PanicBlame(
        "AddIfZero: dead branch became reachable"
      ))(origin),
      origin,
    )

  override def dispatch(program: Program[Pre]): Program[Post] = {
    // Dual of AddIfOne wrap sites: original statement goes in the else arm.
    // Return/goto/break/continue themselves are not sites, but a parent
    // if/loop/block may still be wrapped.
    val candidates = CStatementSites
      .collect(program, excludeContainedAbrupt = false)

    println(s"[AddIfZero] candidate count = ${candidates.size}")

    candidates.zipWithIndex.foreach { case (site, index) =>
      val deepenGenerated = site.target match {
        case loop: Loop[Pre] =>
          s" | deepenGenerated=${DeepenWhileSites.isGenerated(loop)}"
        case _ =>
          ""
      }
      println(
        s"[AddIfZero] candidate $index" + s" | function=${site.functionName}" +
          s" | role=${site.role.label}" + s" | path=${site.path}" +
          s" | kind=${site.description}" + deepenGenerated
      )
    }

    selected = RobustnessSiteSelection.nextIndex(
      "ADD_IF_ZERO_SITE",
      candidates.size,
    ).map { selectedIndex =>
      val site = candidates(selectedIndex)
      println(s"[AddIfZero] SELECTED INDEX = $selectedIndex")
      println(
        s"[AddIfZero] SELECTED" + s" | function=${site.functionName}" +
          s" | role=${site.role.label}" + s" | path=${site.path}" +
          s" | kind=${site.description}"
      )
      site
    }
    wrapped = false

    super.dispatch(program)
  }

  private def isSelected(stat: Statement[Pre]): Boolean =
    selected.exists(site => site.target eq stat)

  override def dispatch(stat: Statement[Pre]): Statement[Post] =
    if (!wrapped && isSelected(stat)) {
      wrapped = true
      if (CStatementSites.isAbruptStatement(stat)) {
        throw new IllegalStateException(
          "AddIfZero selected a forbidden return-like control-flow site: " +
            selected.map(site => s"${site.path} (${site.description})")
              .getOrElse(stat.getClass.getSimpleName)
        )
      }

      println(
        s"[AddIfZero] WRAP class=${stat.getClass.getSimpleName}" +
          s" | ${selected.map(_.description).getOrElse("")}"
      )
      RobustnessStatementWrap.keepSurroundingBraces(
        stat,
        Branch[Post](Seq(
          (bool(value = false, stat.o), deadBranch(stat.o)),
          (
            bool(value = true, stat.o),
            asCompound(stat.rewriteDefault(), stat.o),
          ),
        ))(stat.o),
      )
    } else {
      stat.rewriteDefault()
    }
}
```

### `src/rewrite/vct/rewrite/AddIfOne.scala`

Rewrite `add-if-one`: same collector as AddIfZero; wrap as `if (1) { S } else { assert false }`.

**Old:** this path did not exist.

**Now:**

```scala
package vct.col.rewrite

import vct.col.ast._
import vct.col.ast.RewriteHelpers._
import vct.col.origin.{Origin, PanicBlame}

case object AddIfOne extends RewriterBuilder {
  override def key: String = "addIfOne"

  override def desc: String =
    "Wrap a C statement as if (1) { original } else { assert false }."
}

case class AddIfOne[Pre <: Generation]() extends Rewriter[Pre] {
  private var selected: Option[CStatementSites.Site[Pre]] = None
  private var wrapped: Boolean = false

  private def bool(value: Boolean, origin: Origin): BooleanValue[Post] =
    BooleanValue[Post](value)(origin)

  private def asCompound(
      stat: Statement[Post],
      origin: Origin,
  ): Statement[Post] = RobustnessStatementWrap.asCompound(stat, origin)

  private def deadElse(origin: Origin): Statement[Post] =
    asCompound(
      Assert[Post](BooleanValue[Post](false)(origin))(PanicBlame(
        "AddIfOne: dead else became reachable"
      ))(origin),
      origin,
    )

  override def dispatch(program: Program[Pre]): Program[Post] = {
    // Same wrap-site policy as AddIfZero: return/goto/break/continue
    // themselves are not sites, but a parent if/loop may still be wrapped.
    val candidates = CStatementSites
      .collect(program, excludeContainedAbrupt = false)

    println(s"[AddIfOne] candidate count = ${candidates.size}")

    candidates.zipWithIndex.foreach { case (site, index) =>
      println(
        s"[AddIfOne] candidate $index" + s" | function=${site.functionName}" +
          s" | role=${site.role.label}" + s" | path=${site.path}" +
          s" | kind=${site.description}"
      )
    }

    selected = RobustnessSiteSelection.nextIndex(
      "ADD_IF_ONE_SITE",
      candidates.size,
    ).map { selectedIndex =>
      val site = candidates(selectedIndex)
      println(s"[AddIfOne] SELECTED INDEX = $selectedIndex")
      println(
        s"[AddIfOne] SELECTED" + s" | function=${site.functionName}" +
          s" | role=${site.role.label}" + s" | path=${site.path}" +
          s" | kind=${site.description}"
      )
      site
    }
    wrapped = false

    super.dispatch(program)
  }

  private def isSelected(stat: Statement[Pre]): Boolean =
    selected.exists(site => site.target eq stat)

  override def dispatch(stat: Statement[Pre]): Statement[Post] =
    if (!wrapped && isSelected(stat)) {
      wrapped = true
      if (CStatementSites.isAbruptStatement(stat)) {
        throw new IllegalStateException(
          "AddIfOne selected a forbidden return-like control-flow site: " +
            selected.map(site => s"${site.path} (${site.description})")
              .getOrElse(stat.getClass.getSimpleName)
        )
      }

      println(
        s"[AddIfOne] WRAP class=${stat.getClass.getSimpleName}" +
          s" | ${selected.map(_.description).getOrElse("")}"
      )
      RobustnessStatementWrap.keepSurroundingBraces(
        stat,
        Branch[Post](Seq(
          (
            bool(value = true, stat.o),
            asCompound(stat.rewriteDefault(), stat.o),
          ),
          (bool(value = true, stat.o), deadElse(stat.o)),
        ))(stat.o),
      )
    } else {
      stat.rewriteDefault()
    }
}
```

### `src/rewrite/vct/rewrite/RobustnessForLoopToWhileLoop.scala`

Rewrite `for-to-while`: convert one collected `for` by calling `ForLoopToWhileLoop.rewriteLoop`.

**Old:** this path did not exist.

**Now:**

```scala
package vct.col.rewrite

import vct.col.ast._

case object RobustnessForLoopToWhileLoop extends RewriterBuilder {
  override def key: String = "robustnessForLoopToWhileLoop"

  override def desc: String =
    "Translate one selected continue-free for loop into a while loop for robustness testing."
}

case class RobustnessForLoopToWhileLoop[Pre <: Generation]()
    extends Rewriter[Pre] {
  private var selected: Option[ForLoopToWhileLoopSites.Site[Pre]] = None

  private def selectedSiteIndex(candidates: Seq[_]): Int =
    RobustnessSiteSelection.nextIndex(
      "FOR_TO_WHILE_SITE",
      candidates.size,
    ).getOrElse(0)

  override def dispatch(program: Program[Pre]): Program[Post] = {
    val candidates = ForLoopToWhileLoopSites.collect(program)

    println(s"[ForToWhile] candidate count = ${candidates.size}")

    candidates.zipWithIndex.foreach { case (site, index) =>
      println(
        s"[ForToWhile] candidate $index" +
          s" | function=${site.functionName}" +
          s" | path=${site.path}" +
          s" | kind=${site.description}"
      )
    }

    selected =
      if (candidates.isEmpty) {
        RobustnessSiteSelection.nextIndex(
          "FOR_TO_WHILE_SITE",
          0,
        )
        None
      } else {
        val index = selectedSiteIndex(candidates)
        val site = candidates(index)

        println(s"[ForToWhile] SELECTED INDEX = $index")
        println(
          s"[ForToWhile] SELECTED" +
            s" | function=${site.functionName}" +
            s" | path=${site.path}" +
            s" | kind=${site.description}"
        )

        Some(site)
      }

    super.dispatch(program)
  }

  override def dispatch(stat: Statement[Pre]): Statement[Post] =
    stat match {
      case loop: Loop[Pre] if selected.exists(site => site.target eq loop) =>
        ForLoopToWhileLoop.rewriteLoop(
          loop,
          (loop: Loop[Pre]) => loop.rewriteDefault(),
          (stat: Statement[Pre]) => dispatch(stat),
          (expr: Expr[Pre]) => dispatch(expr),
          (contract: LoopContract[Pre]) => dispatch(contract),
        )

      case _ => stat.rewriteDefault()
    }
}
```

### `src/rewrite/vct/rewrite/RobustnessDeepenWhile.scala`

Rewrite `deepen-while`: nest one collected `while` as outer `while (c)` plus inner `while (nondet() && c)`.

**Old:** this path did not exist.

**Now:**

```scala
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
```

### `src/rewrite/vct/rewrite/RobustnessToMethod.scala`

Rewrite `to-method`: extract one collected block into a new procedure.

**Old:** this path did not exist.

**Now:**

```scala
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

  /** `{ S }` using locals/params `x` becomes `void f(T *x) { S[x := *x] }`
    * and the site becomes `f(&x)`. Each argument gets `\pointer(x, 1, write)`.
    * The procedure is `inline` so original loop contracts still prove after
    * dump and verify.
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
```

### `src/rewrite/vct/rewrite/CStatementSites.scala`

Collector for add-if-zero and add-if-one: wrappable C statements.

**Old:** this path did not exist.

**Now:**

```scala
package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
import vct.col.ref.Ref
import vct.col.resolve.lang.C

/** Wrap sites for add-if-zero and add-if-one. `return` / `goto` / `break` /
  * `continue` themselves are not sites; a parent if, loop, or block still can
  * be.
  */
object CStatementSites {
  sealed trait Role {
    def label: String
  }

  final case class BlockItem(index: Int) extends Role {
    override def label: String = s"BLOCK_ITEM[$index]"
  }

  final case class BranchArm(index: Int) extends Role {
    override def label: String =
      if (index == 0)
        "IF_TRUE"
      else
        s"IF_ELSE[$index]"
  }

  case object LoopBody extends Role {
    override def label: String = "LOOP_BODY"
  }

  final case class Site[G](
      functionName: String,
      path: String,
      role: Role,
      target: Statement[G],
  ) {
    def description: String = describe(target) + " | " + snippet(target)
  }

  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" || name == "abort" || name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  def isAbruptStatement[G](stat: Statement[G]): Boolean =
    stat match {
      case _: Return[G] | _: Throw[G] | _: Break[G] | _: Continue[G] |
          _: Goto[G] | _: CGoto[G] =>
        true
      case _ => false
    }

  def isForbiddenWrapTarget[G](stat: Statement[G]): Boolean =
    stat match {
      // Do not wrap return-like control flow, including an if/loop/block that
      // contains such a statement.
      case _ if isAbruptStatement(stat) => true
      case Scope(_, body) => isForbiddenWrapTarget(body)
      case Block(statements) => statements.exists(isForbiddenWrapTarget)
      case Branch(branches) =>
        branches.exists { case (_, body) => isForbiddenWrapTarget(body) }
      case IndetBranch(branches) => branches.exists(isForbiddenWrapTarget)
      case Loop(_, _, _, _, body) => isForbiddenWrapTarget(body)
      case Switch(_, body) => isForbiddenWrapTarget(body)
      case Label(_, inner, _) => isForbiddenWrapTarget(inner)
      case _ => false
    }

  // True when this node is return-like, or is only a block/scope around one.
  // If/loop containers are allowed so wrapping `if (...) return` stays a site.
  private def containsDirectAbrupt[G](stat: Statement[G]): Boolean =
    stat match {
      case _ if isAbruptStatement(stat) => true
      case Scope(_, body) => containsDirectAbrupt(body)
      case Block(statements) => statements.exists(containsDirectAbrupt)
      case Label(_, inner, _) => containsDirectAbrupt(inner)
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

  private def selectableShape[G](
      stat: Statement[G],
      excludeContainedAbrupt: Boolean,
  ): Boolean =
    stat match {
      case _ if isAbruptStatement(stat) => false
      case _ if excludeContainedAbrupt && isForbiddenWrapTarget(stat) => false
      case _ if !excludeContainedAbrupt && containsDirectAbrupt(stat) => false
      case _ if declarationCluster(stat) => false
      case _: NonExecutableStatement[G] => false

      case _: AssignStmt[G] => true
      case _: Eval[G] => true
      case _: InvocationStatement[G] => true
      case _: Branch[G] => true
      case Block(_) => true
      case Scope(_, Block(_)) => true
      case Scope(_, Loop(_, _, _, _, _)) => true
      case _: Loop[G] => true

      case _ => false
    }

  private def selectable[G](
      role: Role,
      stat: Statement[G],
      excludeContainedAbrupt: Boolean,
  ): Boolean = selectableShape(stat, excludeContainedAbrupt)

  private def snippet[G](stat: Statement[G]): String =
    try {
      implicit val ctx: vct.col.print.Ctx = vct.col.print.Ctx(
        syntax = vct.col.print.Ctx.C,
        width = 80,
      )
      val text = stat.toStringWithContext.replaceAll("\\s+", " ").trim
      if (text.length > 80)
        text.take(77) + "..."
      else
        text
    } catch {
      case _: Throwable => stat.getClass.getSimpleName
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
      case _: AssignStmt[G] => "AssignmentStatement"
      case Eval(expr) =>
        s"ExpressionStatement(expr=${expr.getClass.getSimpleName})"
      case _: InvocationStatement[G] => "InvocationStatement"
      case _: CGoto[G] | _: Goto[G] => "GotoStatement"
      case _: Break[G] => "BreakStatement"
      case _: Continue[G] => "ContinueStatement"
      case _: Return[G] => "ReturnStatement"
      case _: CDeclarationStatement[G] => "DeclarationStatement"
      case other => other.getClass.getSimpleName
    }

  def collect[G](
      program: Program[G],
      excludeContainedAbrupt: Boolean = true,
  ): Seq[Site[G]] = {
    val result = ArrayBuffer.empty[Site[G]]

    def add(
        functionName: String,
        path: String,
        role: Role,
        target: Statement[G],
    ): Unit =
      if (selectable(role, target, excludeContainedAbrupt)) {
        result += Site(functionName, path, role, target)
      }

    def descend(functionName: String, path: String, stat: Statement[G]): Unit =
      stat match {
        case _ if declarationCluster(stat) =>

        case Scope(_, Block(statements)) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            val childPath = s"$path.block[$index]"
            add(functionName, childPath, BlockItem(index), child)
            descend(functionName, childPath, child)
          }

        case Block(statements) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            val childPath = s"$path.block[$index]"
            add(functionName, childPath, BlockItem(index), child)
            descend(functionName, childPath, child)
          }

        case Scope(_, loop @ Loop(_, _, _, _, _)) =>
          descend(functionName, s"$path.loop", loop)

        case Branch(branches) =>
          branches.zipWithIndex.foreach { case ((_, body), index) =>
            add(functionName, s"$path.branch[$index]", BranchArm(index), body)
          }

          branches.zipWithIndex.foreach { case ((_, body), index) =>
            descend(functionName, s"$path.branch[$index]", body)
          }

        case Loop(_, _, _, _, body) =>
          val bodyPath = s"$path.body"
          add(functionName, bodyPath, LoopBody, body)
          descend(functionName, bodyPath, body)

        case IndetBranch(branches) =>
          branches.zipWithIndex.foreach { case (body, index) =>
            add(functionName, s"$path.indet[$index]", BranchArm(index), body)
          }

          branches.zipWithIndex.foreach { case (body, index) =>
            descend(functionName, s"$path.indet[$index]", body)
          }

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
```

### `src/rewrite/vct/rewrite/ForLoopToWhileLoopSites.scala`

Collector for for-to-while: `for` loops without `continue`.

**Old:** this path did not exist.

**Now:**

```scala
package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
import vct.col.resolve.lang.C

/** Sites for for-to-while: `for` loops that do not contain `continue`. */
object ForLoopToWhileLoopSites {
  final case class Site[G](
      functionName: String,
      path: String,
      target: Loop[G],
  ) {
    def description: String =
      s"ForLoop(init=${describe(target.init)}, cond=${target.cond.getClass.getSimpleName}, " +
        s"update=${describe(target.update)}, body=${describe(target.body)})"
  }

  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" || name == "abort" || name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  private def isForLoop[G](loop: Loop[G]): Boolean =
    loop match {
      case Loop(Block(Nil), _, Block(Nil), _, _) => false
      case _ => true
    }

  private def containsContinueForThisLoop[G](stat: Statement[G]): Boolean =
    stat match {
      case _: Continue[G] => true
      case _: Loop[G] => false
      case Block(statements) => statements.exists(containsContinueForThisLoop)
      case Scope(_, body) => containsContinueForThisLoop(body)
      case Branch(branches) =>
        branches.exists { case (_, body) => containsContinueForThisLoop(body) }
      case IndetBranch(branches) =>
        branches.exists(containsContinueForThisLoop)
      case Switch(_, body) => containsContinueForThisLoop(body)
      case Label(_, inner, _) => containsContinueForThisLoop(inner)
      case _ => false
    }

  private def selectable[G](loop: Loop[G]): Boolean =
    isForLoop(loop) && !containsContinueForThisLoop(loop.body)

  private def describe[G](stat: Statement[G]): String =
    stat match {
      case Block(stats) => s"Block(items=${stats.size})"
      case Scope(locals, Block(stats)) =>
        s"Scope(locals=${locals.size}, items=${stats.size})"
      case Scope(_, body) => s"Scope(body=${body.getClass.getSimpleName})"
      case Loop(init, _, update, _, body) =>
        s"Loop(init=${init.getClass.getSimpleName}, " +
          s"update=${update.getClass.getSimpleName}, " +
          s"body=${body.getClass.getSimpleName})"
      case Branch(branches) => s"IfStatement(arms=${branches.size})"
      case _: AssignInitial[G] => "InitialAssignmentStatement"
      case _: AssignStmt[G] => "AssignmentStatement"
      case Eval(expr) =>
        s"ExpressionStatement(expr=${expr.getClass.getSimpleName})"
      case _: InvocationStatement[G] => "InvocationStatement"
      case _: Continue[G] => "ContinueStatement"
      case _: Return[G] => "ReturnStatement"
      case _: CDeclarationStatement[G] | _: LocalDecl[G] => "DeclarationStatement"
      case _: NonExecutableStatement[G] => "NonExecutableStatement"
      case other => other.getClass.getSimpleName
    }

  def collect[G](program: Program[G]): Seq[Site[G]] = {
    val result = ArrayBuffer.empty[Site[G]]

    def add(functionName: String, path: String, loop: Loop[G]): Unit =
      if (selectable(loop)) {
        result += Site(functionName, path, loop)
      }

    def descend(functionName: String, path: String, stat: Statement[G]): Unit =
      stat match {
        case block @ Block(statements) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            descend(functionName, s"$path.block[$index]", child)
          }

        case Scope(_, body) =>
          descend(functionName, s"$path.scope", body)

        case loop @ Loop(_, _, _, _, body) =>
          add(functionName, path, loop)
          descend(functionName, s"$path.body", body)

        case Branch(branches) =>
          branches.zipWithIndex.foreach { case ((_, body), index) =>
            descend(functionName, s"$path.branch[$index]", body)
          }

        case IndetBranch(branches) =>
          branches.zipWithIndex.foreach { case (body, index) =>
            descend(functionName, s"$path.indet[$index]", body)
          }

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
```

### `src/rewrite/vct/rewrite/DeepenWhileSites.scala`

Collector for deepen-while: `while` with `LoopInvariant`; skips the generated inner adapter.

**Old:** this path did not exist.

**Now:**

```scala
package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
import vct.col.origin.LabelContext
import vct.col.resolve.lang.C

/** Places deepen-while may touch: a `while` whose contract is
  * `LoopInvariant`, with no assignment or call in the condition, and no
  * `break` / `continue` / `goto` for this loop. A `for` is not a site.
  * `IterationContract` is not a site.
  *
  * Only the inner adapter while created by deepen-while is excluded here.
  * The selected outer while stays a candidate, so `--robustness-repeat`
  * may nest another adapter under the same `while (C)`. Other transforms
  * do not read this marker.
  */
object DeepenWhileSites {
  val GeneratedLabel: String = "deepen-while-generated"
  final case class Site[G](
      functionName: String,
      path: String,
      target: Loop[G],
  ) {
    def description: String =
      s"While(generated=${isGenerated(target)}, cond=${target.cond.getClass.getSimpleName}, body=${describe(target.body)})"
  }

  def isWhile[G](loop: Loop[G]): Boolean =
    loop match {
      case Loop(Block(Nil), _, Block(Nil), _, _) => true
      case _ => false
    }

  def isGenerated[G](loop: Loop[G]): Boolean =
    loop.o.originContents.exists {
      case LabelContext(GeneratedLabel) => true
      case _ => false
    }

  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" || name == "abort" || name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  private def containsForbiddenControl[G](
      stat: Statement[G],
      inSwitch: Boolean = false,
  ): Boolean =
    stat match {
      case _: Break[G] => !inSwitch
      case _: Continue[G] | _: Goto[G] | _: CGoto[G] => true
      case _: Loop[G] => false
      case Block(statements) =>
        statements.exists(containsForbiddenControl(_, inSwitch))
      case Scope(_, body) => containsForbiddenControl(body, inSwitch)
      case Branch(branches) =>
        branches.exists { case (_, body) =>
          containsForbiddenControl(body, inSwitch)
        }
      case IndetBranch(branches) =>
        branches.exists(containsForbiddenControl(_, inSwitch))
      case Switch(_, body) => containsForbiddenControl(body, inSwitch = true)
      case Label(_, inner, _) => containsForbiddenControl(inner, inSwitch)
      case _ => false
    }

  private def condHasAssignOrCall[G](expr: Expr[G]): Boolean = {
    def go(node: Node[G]): Boolean =
      node match {
        case _: AssignExpression[G] => true
        case _: InvokingNode[G] => true
        case other => other.subnodes.exists(go)
      }
    go(expr)
  }

  def selectable[G](loop: Loop[G]): Boolean =
    isWhile(loop) && !isGenerated(loop) && (loop.contract match {
      case _: LoopInvariant[G] => true
      case _: IterationContract[G] => false
    }) && !containsForbiddenControl(loop.body) &&
      !condHasAssignOrCall(loop.cond)

  private def describe[G](stat: Statement[G]): String =
    stat match {
      case Block(stats) => s"Block(items=${stats.size})"
      case Scope(locals, Block(stats)) =>
        s"Scope(locals=${locals.size}, items=${stats.size})"
      case Scope(_, body) => s"Scope(body=${body.getClass.getSimpleName})"
      case _ => stat.getClass.getSimpleName
    }

  def collect[G](program: Program[G]): Seq[Site[G]] = {
    val result = ArrayBuffer.empty[Site[G]]

    def add(functionName: String, path: String, loop: Loop[G]): Unit = {
      if (isWhile(loop) && isGenerated(loop)) {
        println(
          s"[DeepenWhile] skip generated adapter" +
            s" | function=$functionName" +
            s" | path=$path"
        )
      }
      if (selectable(loop)) {
        result += Site(functionName, path, loop)
      }
    }

    def descend(functionName: String, path: String, stat: Statement[G]): Unit =
      stat match {
        case Block(statements) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            descend(functionName, s"$path.block[$index]", child)
          }

        case Scope(_, body) =>
          descend(functionName, s"$path.scope", body)

        case loop @ Loop(_, _, _, _, body) =>
          add(functionName, path, loop)
          descend(functionName, s"$path.body", body)

        case Branch(branches) =>
          branches.zipWithIndex.foreach { case ((_, body), index) =>
            descend(functionName, s"$path.branch[$index]", body)
          }

        case IndetBranch(branches) =>
          branches.zipWithIndex.foreach { case (body, index) =>
            descend(functionName, s"$path.indet[$index]", body)
          }

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
```

### `src/rewrite/vct/rewrite/ToMethodSites.scala`

Collector for to-method: blocks that can be extracted.

**Old:** this path did not exist.

**Now:**

```scala
package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
import vct.col.ref.Ref
import vct.col.resolve.ctx.{RefCLocalDeclaration, RefCParam, RefVariable}
import vct.col.resolve.lang.C

/** Places the to-method rewrite may touch: a `{ ... }` that has no
  * `return`/`break`/`continue`/`goto`, no declaration, and no nested loop.
  * An `if` inside is fine. Assignments and `--`/`++` are not sites by
  * themselves (only the surrounding braces).
  */
object ToMethodSites {
  final case class Site[G](
      functionName: String,
      path: String,
      target: Statement[G],
  ) {
    def description: String = describe(target)
  }

  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" || name == "abort" || name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  def isCompound[G](stat: Statement[G]): Boolean =
    stat match {
      case Block(stmts) if stmts.nonEmpty => true
      case Scope(_, Block(stmts)) if stmts.nonEmpty => true
      case Scope(Nil, body) => isCompound(body)
      case _ => false
    }

  private def forbidden[G](node: Node[G]): Boolean =
    node match {
      case _: Return[G] | _: Throw[G] | _: Break[G] | _: Continue[G] |
          _: Goto[G] | _: CGoto[G] =>
        true
      case _: Loop[G] => true
      case _: Switch[G] => true
      case _: LocalDecl[G] | _: HeapLocalDecl[G] |
          _: CDeclarationStatement[G] =>
        true
      case Scope(locals, _) if locals.nonEmpty => true
      case other => other.subnodes.exists(forbidden)
    }

  def selectable[G](stat: Statement[G]): Boolean =
    isCompound(stat) && !forbidden(stat)

  def isCapturedLocal[G](local: CLocal[G]): Boolean =
    local.ref match {
      case Some(_: RefCParam[G]) => true
      case Some(_: RefCLocalDeclaration[G]) => true
      case Some(RefVariable(_)) => true
      case _ => false
    }

  def capturedVars[G](stat: Statement[G]): Seq[Variable[G]] = {
    val result = ArrayBuffer.empty[Variable[G]]
    val seen = ArrayBuffer.empty[Node[G]]

    def go(node: Node[G]): Unit = {
      if (seen.exists(_ eq node))
        return
      seen += node

      node match {
        case Local(Ref(v)) =>
          if (!result.exists(_ eq v))
            result += v
        case _: Type[G] =>
        case _ =>
          node.subnodes.foreach(go)
      }
    }

    go(stat)
    result.toSeq
  }

  private def describe[G](stat: Statement[G]): String =
    stat match {
      case Block(stats) => s"Compound(items=${stats.size})"
      case Scope(locals, Block(stats)) =>
        s"Compound(locals=${locals.size}, items=${stats.size})"
      case Scope(_, body) => s"Compound(body=${body.getClass.getSimpleName})"
      case _ => stat.getClass.getSimpleName
    }

  def collect[G](program: Program[G]): Seq[Site[G]] = {
    val result = ArrayBuffer.empty[Site[G]]

    def add(functionName: String, path: String, stat: Statement[G]): Unit =
      if (selectable(stat)) {
        result += Site(functionName, path, stat)
      }

    def descend(functionName: String, path: String, stat: Statement[G]): Unit =
      stat match {
        case Scope(_, Block(statements)) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            val childPath = s"$path.block[$index]"
            add(functionName, childPath, child)
            descend(functionName, childPath, child)
          }

        case Block(statements) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            val childPath = s"$path.block[$index]"
            add(functionName, childPath, child)
            descend(functionName, childPath, child)
          }

        case Scope(_, loop @ Loop(_, _, _, _, _)) =>
          descend(functionName, s"$path.loop", loop)

        case Branch(branches) =>
          branches.zipWithIndex.foreach { case ((_, body), index) =>
            add(functionName, s"$path.branch[$index]", body)
            descend(functionName, s"$path.branch[$index]", body)
          }

        case Loop(_, _, _, _, body) =>
          val bodyPath = s"$path.body"
          add(functionName, bodyPath, body)
          descend(functionName, bodyPath, body)

        case IndetBranch(branches) =>
          branches.zipWithIndex.foreach { case (body, index) =>
            add(functionName, s"$path.indet[$index]", body)
            descend(functionName, s"$path.indet[$index]", body)
          }

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
```

### `src/rewrite/vct/rewrite/RobustnessSiteSelection.scala`

Picks one site index per application from `*_SITE` or 0; spin uses `forceNext`.

**Old:** this path did not exist.

**Now:**

```scala
package vct.col.rewrite

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

/** Picks one site index per robustness application of a named transform.
  *
  * `ADD_IF_ZERO_SITE=0,2,1` with `--robustness-repeat 3` uses 0 then 2 then 1.
  * A shorter list reuses its last index on later applications.
  *
  * If the env var is unset, each application selects site 0. Counters are
  * per env name so mixed transform chains do not share site lists.
  *
  * `forceNext` is consumed once by the following `nextIndex` call and wins
  * over the env list. Spin uses that so a random site can ride the existing
  * rewriters.
  *
  * If an application has no remaining candidates, it is a no-op.
  */
object RobustnessSiteSelection {
  private val applications = new ConcurrentHashMap[String, AtomicInteger]()
  private val forced = new AtomicReference[Integer](null)

  def reset(): Unit = {
    applications.clear()
    forced.set(null)
  }

  def forceNext(index: Int): Unit = forced.set(Integer.valueOf(index))

  def nextIndex(
      envName: String,
      candidateCount: Int,
  ): Option[Int] = {
    val round = applications.computeIfAbsent(
      envName,
      _ => new AtomicInteger(0),
    ).getAndIncrement()

    if (candidateCount <= 0) {
      forced.set(null)
      println(s"[$envName] application ${round + 1}: 0 candidates, skip")
      return None
    }

    Option(forced.getAndSet(null)).map(_.intValue) match {
      case Some(selected) =>
        if (selected < 0 || selected >= candidateCount) {
          throw new IllegalArgumentException(
            s"Invalid forced $envName index $selected for application ${round + 1}. " +
              s"Valid range: 0..${candidateCount - 1}"
          )
        }
        println(
          s"[$envName] application ${round + 1}: spin site $selected / $candidateCount"
        )
        return Some(selected)
      case None =>
    }

    sys.env.get(envName).map(_.trim).filter(_.nonEmpty) match {
      case Some(raw) =>
        val parts =
          raw.split("[,\\s]+").iterator.filter(_.nonEmpty).map { token =>
            try { token.toInt }
            catch {
              case _: NumberFormatException =>
                throw new IllegalArgumentException(
                  s"$envName must be an integer or a comma-separated list of integers, got: $raw"
                )
            }
          }.toSeq

        if (parts.isEmpty) {
          throw new IllegalArgumentException(s"$envName is empty")
        }

        val selected =
          if (round < parts.size)
            parts(round)
          else
            parts.last

        if (selected < 0 || selected >= candidateCount) {
          throw new IllegalArgumentException(
            s"Invalid $envName index $selected for application ${round + 1}. " +
              s"Valid range: 0..${candidateCount - 1}"
          )
        }

        println(
          s"[$envName] application ${round + 1}: selected $selected / $candidateCount"
        )
        Some(selected)

      case None =>
        println(
          s"[$envName] application ${round + 1}: default site 0 / $candidateCount"
        )
        Some(0)
    }
  }
}
```

### `src/rewrite/vct/rewrite/RobustnessSpin.scala`

One mixed step: uniform choice of kind among remaining kinds with sites, then a uniform site index.

**Old:** this path did not exist.

**Now:**

```scala
package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

import vct.col.ast.Program

/** One mixed-mode spin step: pick a transform kind, then a site.
  *
  * Each kind in the pool has weight 1, so the pick among remaining kinds is
  * uniform. A kind with no sites is dropped and another kind is tried. If
  * nothing in the pool applies, later repeats are no-ops. The site index is
  * uniform among that kind's current candidates. The rewriter is one of
  * AddIfZero, AddIfOne, RobustnessForLoopToWhileLoop, RobustnessDeepenWhile,
  * or RobustnessToMethod (the last only if it is in the pool).
  */
object RobustnessSpin {
  sealed trait Kind {
    def label: String
    def candidateCount[Pre <: Generation](program: Program[Pre]): Int
    def apply[Pre <: Generation](program: Program[Pre]): Program[Rewritten[Pre]]
  }

  case object AddIfZeroKind extends Kind {
    override def label: String = "add-if-zero"
    override def candidateCount[Pre <: Generation](
        program: Program[Pre]
    ): Int =
      CStatementSites.collect(program, excludeContainedAbrupt = false).size
    override def apply[Pre <: Generation](
        program: Program[Pre]
    ): Program[Rewritten[Pre]] = AddIfZero[Pre]().dispatch(program)
  }

  case object AddIfOneKind extends Kind {
    override def label: String = "add-if-one"
    override def candidateCount[Pre <: Generation](
        program: Program[Pre]
    ): Int =
      CStatementSites.collect(program, excludeContainedAbrupt = false).size
    override def apply[Pre <: Generation](
        program: Program[Pre]
    ): Program[Rewritten[Pre]] = AddIfOne[Pre]().dispatch(program)
  }

  case object ForToWhileKind extends Kind {
    override def label: String = "for-to-while"
    override def candidateCount[Pre <: Generation](
        program: Program[Pre]
    ): Int = ForLoopToWhileLoopSites.collect(program).size
    override def apply[Pre <: Generation](
        program: Program[Pre]
    ): Program[Rewritten[Pre]] =
      RobustnessForLoopToWhileLoop[Pre]().dispatch(program)
  }

  case object DeepenWhileKind extends Kind {
    override def label: String = "deepen-while"
    override def candidateCount[Pre <: Generation](
        program: Program[Pre]
    ): Int = DeepenWhileSites.collect(program).size
    override def apply[Pre <: Generation](
        program: Program[Pre]
    ): Program[Rewritten[Pre]] =
      RobustnessDeepenWhile[Pre]().dispatch(program)
  }

  case object ToMethodKind extends Kind {
    override def label: String = "to-method"
    override def candidateCount[Pre <: Generation](
        program: Program[Pre]
    ): Int = ToMethodSites.collect(program).size
    override def apply[Pre <: Generation](
        program: Program[Pre]
    ): Program[Rewritten[Pre]] = RobustnessToMethod[Pre]().dispatch(program)
  }

  val defaultPool: Seq[Kind] =
    Seq(
      AddIfZeroKind,
      AddIfOneKind,
      ForToWhileKind,
      DeepenWhileKind,
    )

  def kindOf(builder: RewriterBuilder): Kind =
    builder match {
      case AddIfZero => AddIfZeroKind
      case AddIfOne => AddIfOneKind
      case RobustnessForLoopToWhileLoop => ForToWhileKind
      case RobustnessDeepenWhile => DeepenWhileKind
      case RobustnessToMethod => ToMethodKind
      case other =>
        throw new IllegalArgumentException(
          s"Robustness spin pool cannot include ${other.key}"
        )
    }

  private val lock = new Object
  private var rng: Random = new Random()
  private var pool: Seq[Kind] = defaultPool
  private var stopped: Boolean = false
  private var round: Int = 0
  private val recordedTrace = ArrayBuffer.empty[String]

  def reset(seed: Long, kinds: Seq[Kind]): Unit =
    lock.synchronized {
      rng = new Random(seed)
      pool = if (kinds.isEmpty)
        defaultPool
      else
        kinds
      stopped = false
      round = 0
      recordedTrace.clear()
    }

  def trace: Seq[String] = lock.synchronized { recordedTrace.toSeq }

  def step[Pre <: Generation](program: Program[Pre]): Option[Kind] =
    lock.synchronized {
      if (stopped)
        return None

      round += 1
      var remaining = pool.map(kind => (kind, 1)).toList

      while (remaining.nonEmpty) {
        val choice = weightedChoice(remaining)
        val n = choice.candidateCount(program)
        if (n > 0) {
          val site = rng.nextInt(n)
          RobustnessSiteSelection.forceNext(site)
          val line = s"${choice.label}: $site"
          recordedTrace += line
          println(s"[RobustnessSpin] round $round: $line / $n")
          return Some(choice)
        }
        println(
          s"[RobustnessSpin] round $round: ${choice.label} has 0 candidates, retry"
        )
        remaining = remaining.filter { case (kind, _) => kind != choice }
      }

      println(
        s"[RobustnessSpin] round $round: no applicable transform, stopping"
      )
      stopped = true
      None
    }

  private def weightedChoice(items: Seq[(Kind, Int)]): Kind = {
    val total = items.map(_._2).sum
    var r = rng.nextInt(total)
    for ((kind, weight) <- items) {
      if (r < weight)
        return kind
      r -= weight
    }
    items.last._1
  }
}

case object RobustnessSpinStep extends RewriterBuilder {
  override def key: String = "robustnessSpinStep"
  override def desc: String =
    "One random robustness transform at a random applicable site."
}

case class RobustnessSpinStep[Pre <: Generation]() extends Rewriter[Pre] {
  override def dispatch(program: Program[Pre]): Program[Post] =
    RobustnessSpin.step(program) match {
      case None => super.dispatch(program)
      case Some(kind) => kind.apply(program)
    }
}
```

### `src/rewrite/vct/rewrite/RobustnessStatementWrap.scala`

Keeps `{ }` when a Block/Scope body is replaced by a node the C printer would not brace.

**Old:** this path did not exist.

**Now:**

```scala
package vct.col.rewrite

import vct.col.ast._
import vct.col.origin.Origin

/** Shared wrap helpers for AddIfZero, AddIfOne, deepen-while, and to-method. */
object RobustnessStatementWrap {
  def asCompound[G](stat: Statement[G], origin: Origin): Statement[G] =
    stat match {
      case scope @ Scope(Nil, Block(_)) => scope
      case other => Scope[G](Nil, Block[G](Seq(other))(origin))(origin)
    }

  /**
   * Loop/if printers only emit `{ }` for Block/Scope bodies. Wrapping a
   * compound site (LOOP_BODY, then-arm, ...) replaces that Scope with a
   * Branch; without this, C prints `for (...) if (0)` as if the wrap sat
   * in the loop header.
   */
  def keepSurroundingBraces[Pre, Post](
      original: Statement[Pre],
      wrapped: Statement[Post],
  ): Statement[Post] =
    original match {
      case Scope(_, Block(_)) | Block(_) => asCompound(wrapped, wrapped.o)
      case _ => wrapped
    }
}
```

### `util/robustness/run-benchmark.sh`

Driver: ordinary `vercors` on originals, `vercors --robustness` for the dump, ordinary `vercors` on mutants. Computes TSV verdicts.

**Old:** this path did not exist.

**Now:**

```bash
#!/usr/bin/env bash
# Driver, not VerCors itself:
#   1) ordinary vercors on each original .c
#   2) vercors --robustness (dump only, no Silicon)
#   3) ordinary vercors on each mutant .c
# Edit util/robustness/config.env, then run this script with no arguments.
# Values in this file are fallbacks if a key is missing from the config.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DUMP_NAME="robustness-transformed.c"
DEFAULT_VCT="$ROOT/out/vercors/main/runScript.dest/vercors"
DEFAULT_INPUT="$ROOT/benchmark"
DEFAULT_OUT="$ROOT/robustness-results/post-resolution"
ALL_SITE_VARS=(
  ADD_IF_ZERO_SITE ADD_IF_ONE_SITE FOR_TO_WHILE_SITE
  DEEPEN_WHILE_SITE TO_METHOD_SITE
)

MODE="preferred"
TRANSFORM="add-if-zero"
TRANSFORM_SET=0
REPEAT=1
SEED=""
SITES="all"
INPUT="$DEFAULT_INPUT"
OUT="$DEFAULT_OUT"
VCT=""
PROGRAMS=""
TIMEOUT=""
VERIFY_MUTANTS=1
VERIFY_ORIGINALS=1
STAMP=0
DRY_RUN=0
FORCE_ORIGINAL=0
CONFIG_FILE="$SCRIPT_DIR/config.env"

die() { echo "ERROR: $*" >&2; exit 1; }
log() { echo "[$(date +%H:%M:%S)] $*"; }

resolve_path() {
  local p="$1"
  if [[ -z "$p" ]]; then
    echo "$p"
  elif [[ "$p" == /* ]]; then
    echo "$p"
  else
    echo "$ROOT/$p"
  fi
}

load_config() {
  local file="$1" line key value
  [[ -f "$file" ]] || return 0
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%%#*}"
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    [[ -z "$line" || "$line" != *=* ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    value="${value#\"}"
    value="${value%\"}"
    value="${value#\'}"
    value="${value%\'}"
    case "$key" in
      MODE) MODE="$value" ;;
      TRANSFORM) TRANSFORM="$value"; TRANSFORM_SET=1 ;;
      REPEAT) REPEAT="$value" ;;
      SEED) SEED="$value" ;;
      SITES) SITES="$value" ;;
      INPUT) INPUT="$value" ;;
      OUT) OUT="$value" ;;
      PROGRAMS) PROGRAMS="$value" ;;
      VCT) VCT="$value" ;;
      TIMEOUT) TIMEOUT="$value" ;;
      VERIFY_MUTANTS) VERIFY_MUTANTS="$value" ;;
      VERIFY_ORIGINALS) VERIFY_ORIGINALS="$value" ;;
      FORCE_ORIGINAL) FORCE_ORIGINAL="$value" ;;
      STAMP) STAMP="$value" ;;
      DRY_RUN) DRY_RUN="$value" ;;
    esac
  done < "$file"
}

usage() {
  cat <<'EOF'
Usage:  ./util/robustness/run-benchmark.sh

Edit util/robustness/config.env, then run the command above.
A flag on the command line overrides the matching config value.

--config PATH
--mode preferred|multiple|mixed|chain|verify-only
--transform add-if-zero|add-if-one|for-to-while|deepen-while|to-method
--repeat N   --seed N   --sites all|0,2,5
--input PATH  --out PATH  --programs 1_absolute,2_swap
--vercors PATH  --timeout SEC
--skip-verify  --skip-original  --force-original  --stamp  --dry-run
EOF
}

site_var_for() {
  case "$1" in
    add-if-zero|add_if_zero) echo ADD_IF_ZERO_SITE ;;
    add-if-one|add_if_one) echo ADD_IF_ONE_SITE ;;
    for-to-while|for_to_while) echo FOR_TO_WHILE_SITE ;;
    deepen-while|deepen_while) echo DEEPEN_WHILE_SITE ;;
    to-method|to_method) echo TO_METHOD_SITE ;;
    *) echo "" ;;
  esac
}

clear_robustness_env() {
  local v
  for v in "${ALL_SITE_VARS[@]}" ROBUSTNESS_TRANSFORM ROBUSTNESS_SPIN ROBUSTNESS_SEED; do
    unset "$v"
  done
}

verify_status() {
  local logf="$1"
  if [[ "$DRY_RUN" == "1" ]]; then echo DRY; return; fi
  if [[ -f "$logf" ]] && grep -q "Verification completed successfully" "$logf"; then
    echo PASS
  else
    echo FAIL
  fi
}

transform_status() {
  local logf="$1"
  if [[ "$DRY_RUN" == "1" ]]; then echo DRY; return; fi
  if [[ -f "$logf" ]] && grep -q "Robustness transformation completed" "$logf"; then
    echo PASS
  else
    echo FAIL
  fi
}

candidate_count() {
  local n
  n="$(grep -E 'candidate count = [0-9]+' "$1" 2>/dev/null | head -n 1 | grep -oE '[0-9]+$' || true)"
  echo "${n:-0}"
}

spin_trace() {
  grep '\[RobustnessSpin\] round' "$1" 2>/dev/null \
    | grep -v 'has 0 candidates' | grep -v 'stopping' \
    | sed -E 's/.*round [0-9]+: ([a-z0-9-]+: [0-9]+) \/.*/\1/' \
    | paste -sd '; ' - || true
}

verdict() {
  local original="$1" tstatus="$2" vstatus="$3" ncand="$4"
  if [[ "$ncand" == "0" ]]; then echo NO_SITE
  elif [[ "$tstatus" != "PASS" ]]; then echo TRANSFORM_FAIL
  elif [[ "$original" != "PASS" ]]; then echo SKIP
  elif [[ "$vstatus" == "PASS" ]]; then echo ROBUST
  else echo NOT_ROBUST
  fi
}

collect_files() {
  local path="$1" f base
  FILES=()
  if [[ -f "$path" ]]; then
    FILES+=("$path")
  elif [[ -d "$path" ]]; then
    shopt -s nullglob
    for f in "$path"/*.c; do
      base="$(basename "$f" .c)"
      if [[ -n "$PROGRAMS" ]]; then
        case ",$PROGRAMS," in
          *,"$base",*) ;;
          *) continue ;;
        esac
      fi
      FILES+=("$f")
    done
    shopt -u nullglob
  else
    die "input not found: $path"
  fi
  [[ ${#FILES[@]} -gt 0 ]] || die "no .c files in: $path"
}

find_vct() {
  if [[ -n "$VCT" ]]; then
    [[ -x "$VCT" ]] || die "--vercors is not executable: $VCT"
    return
  fi
  if [[ -n "${VERCORS:-}" && -x "${VERCORS}" ]]; then VCT="$VERCORS"; return; fi
  if [[ -x "$DEFAULT_VCT" ]]; then VCT="$DEFAULT_VCT"; return; fi
  if [[ -x "$ROOT/bin/vct" ]]; then VCT="$ROOT/bin/vct"; return; fi
  die "vercors not found. Run ./mill -j 0 vercors.main.runScript first."
}

run_verify() {
  local src="$1" logf="$2" extra=()
  if [[ -n "$TIMEOUT" ]]; then extra+=(--dev-total-timeout "$TIMEOUT"); fi
  if [[ "$DRY_RUN" == "1" ]]; then
    echo "DRY  $VCT ${extra[*]} $src"
    return 0
  fi
  clear_robustness_env
  set +e
  "$VCT" "${extra[@]}" "$src" > "$logf" 2>&1
  set -e
}

run_transform() {
  local src="$1" logf="$2"
  shift 2
  if [[ "$DRY_RUN" == "1" ]]; then
    echo "DRY  $VCT --robustness $* $src"
    return 0
  fi
  local work
  work="$(mktemp -d)"
  (
    cd "$work"
    set +e
    "$VCT" --robustness "$@" "$src" > "$logf" 2>&1
    set -e
  )
  LAST_WORK="$work"
  LAST_DUMP="$work/$DUMP_NAME"
}

copy_dump() {
  local dest="$1"
  if [[ "$DRY_RUN" == "1" ]]; then echo "DRY  cp dump -> $dest"; return 0; fi
  if [[ -f "${LAST_DUMP:-}" ]]; then cp -f "$LAST_DUMP" "$dest"; fi
  if [[ -n "${LAST_WORK:-}" && -d "$LAST_WORK" ]]; then rm -rf "$LAST_WORK"; fi
  LAST_WORK=""
  LAST_DUMP=""
}

_scan=("$@")
for ((i = 0; i < ${#_scan[@]}; i++)); do
  if [[ "${_scan[i]}" == "--config" ]]; then
    CONFIG_FILE="${_scan[i+1]:-}"
  fi
done
load_config "$CONFIG_FILE"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --config) CONFIG_FILE="${2:-}"; shift 2 ;;
    --mode) MODE="${2:-}"; shift 2 ;;
    --transform) TRANSFORM="${2:-}"; TRANSFORM_SET=1; shift 2 ;;
    --repeat) REPEAT="${2:-}"; shift 2 ;;
    --seed) SEED="${2:-}"; shift 2 ;;
    --sites) SITES="${2:-}"; shift 2 ;;
    --input) INPUT="${2:-}"; shift 2 ;;
    --out) OUT="${2:-}"; shift 2 ;;
    --vercors) VCT="${2:-}"; shift 2 ;;
    --programs) PROGRAMS="${2:-}"; shift 2 ;;
    --timeout) TIMEOUT="${2:-}"; shift 2 ;;
    --skip-verify) VERIFY_MUTANTS=0; shift ;;
    --skip-original) VERIFY_ORIGINALS=0; shift ;;
    --force-original) FORCE_ORIGINAL=1; shift ;;
    --stamp) STAMP=1; shift ;;
    --dry-run) DRY_RUN=1; shift ;;
    --help|-h) usage; exit 0 ;;
    *) die "unknown flag: $1" ;;
  esac
done

case "$MODE" in
  preferred|multiple|mixed|chain|verify-only) ;;
  *) die "--mode must be one of: preferred multiple mixed chain verify-only" ;;
esac
[[ "$REPEAT" =~ ^[0-9]+$ ]] && [[ "$REPEAT" -ge 1 ]] || die "--repeat must be a positive integer"

PROGRAMS="${PROGRAMS// /}"
TRANSFORM="${TRANSFORM// /}"
INPUT="$(resolve_path "$INPUT")"
OUT="$(resolve_path "$OUT")"
if [[ -n "$VCT" ]]; then VCT="$(resolve_path "$VCT")"; fi
if [[ "$STAMP" == "1" ]]; then
  OUT="$OUT/run-$(date +%Y%m%d-%H%M%S)"
fi

find_vct
collect_files "$INPUT"
mkdir -p "$OUT"
{
  echo "date=$(date -Iseconds)"
  echo "mode=$MODE transform=$TRANSFORM repeat=$REPEAT seed=${SEED:-}"
  echo "sites=$SITES input=$INPUT out=$OUT vct=$VCT files=${#FILES[@]}"
} > "$OUT/run.txt"

log "mode=$MODE  transform=$TRANSFORM  files=${#FILES[@]}  out=$OUT"

ORIG_DIR="$OUT/_original-verify"
mkdir -p "$ORIG_DIR"
declare -A ORIG_STATUS

verify_originals() {
  local src base dest st
  log "original verify"
  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    dest="$ORIG_DIR/${base}.verify.log"
    if [[ "$VERIFY_ORIGINALS" != "1" ]]; then
      ORIG_STATUS["$base"]="SKIPPED"
      continue
    fi
    if [[ "$FORCE_ORIGINAL" != "1" && -f "$dest" ]]; then
      ORIG_STATUS["$base"]="$(verify_status "$dest")"
      log "  reuse $base  original=${ORIG_STATUS[$base]}"
      continue
    fi
    log "  verify $base"
    run_verify "$src" "$dest"
    ORIG_STATUS["$base"]="$(verify_status "$dest")"
    log "  ${ORIG_STATUS[$base]}  $base"
  done
}

if [[ "$MODE" == "verify-only" ]]; then
  verify_originals
  echo "program	original"
  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    echo "$base	${ORIG_STATUS[$base]}"
  done | tee "$OUT/summary.tsv"
  exit 0
fi

verify_originals

run_preferred() {
  local src base dest n i site_list sv tlog vlog tst vst vd tpass tfail vpass vfail disc_dump
  local first_tf="${TRANSFORM%%,*}"
  [[ "$TRANSFORM" == "$first_tf" ]] || die "preferred takes one transform; use --mode chain for a list"
  sv="$(site_var_for "$first_tf")"
  [[ -n "$sv" ]] || die "unknown transform: $TRANSFORM"

  local tdir="$OUT/$first_tf" summary verdicts
  mkdir -p "$tdir"
  summary="$tdir/summary.tsv"
  verdicts="$tdir/verdict.tsv"
  echo "program	original	candidates	transform_pass	transform_fail	verify_pass	verify_fail" > "$summary"
  echo "program	site	original	transform	verify	verdict" > "$verdicts"

  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    dest="$tdir/$base"
    mkdir -p "$dest"
    if [[ -f "$ORIG_DIR/${base}.verify.log" ]]; then
      cp -f "$ORIG_DIR/${base}.verify.log" "$dest/original.verify.log"
    fi
    tpass=0; tfail=0; vpass=0; vfail=0

    log "  $base  discovery"
    clear_robustness_env
    export ROBUSTNESS_TRANSFORM="$first_tf"
    export "$sv=0"
    run_transform "$src" "$dest/discovery.log"
    n="$(candidate_count "$dest/discovery.log")"
    disc_dump=""
    if [[ -f "${LAST_DUMP:-}" ]]; then
      disc_dump="$(mktemp)"
      cp -f "$LAST_DUMP" "$disc_dump"
    fi
    if [[ -n "${LAST_WORK:-}" && -d "$LAST_WORK" ]]; then rm -rf "$LAST_WORK"; fi
    LAST_WORK=""; LAST_DUMP=""

    site_list=()
    if [[ "$SITES" == "all" ]]; then
      if [[ "$n" -gt 0 ]]; then
        for ((i = 0; i < n; i++)); do site_list+=("$i"); done
      fi
    else
      IFS=',' read -r -a site_list <<< "$SITES"
    fi

    if [[ ${#site_list[@]} -eq 0 ]]; then
      echo "$base	${ORIG_STATUS[$base]}	$n	0	0	0	0" >> "$summary"
      echo "$base	-	${ORIG_STATUS[$base]}	SKIP	SKIP	NO_SITE" >> "$verdicts"
      [[ -n "$disc_dump" ]] && rm -f "$disc_dump"
      continue
    fi

    for i in "${site_list[@]}"; do
      tlog="$dest/site-${i}.transform.log"
      vlog="$dest/site-${i}.verify.log"
      if [[ "$n" -gt 0 && "$i" -ge "$n" ]]; then
        echo "$base	$i	${ORIG_STATUS[$base]}	FAIL	SKIP	TRANSFORM_FAIL" >> "$verdicts"
        tfail=$((tfail + 1))
        continue
      fi
      if [[ "$i" == "0" && -f "$dest/discovery.log" ]]; then
        cp -f "$dest/discovery.log" "$tlog"
        if [[ -n "$disc_dump" && -f "$disc_dump" ]]; then
          cp -f "$disc_dump" "$dest/site-0.c"
        fi
      else
        log "  $base  transform site=$i"
        clear_robustness_env
        export ROBUSTNESS_TRANSFORM="$first_tf"
        export "$sv=$i"
        run_transform "$src" "$tlog"
        copy_dump "$dest/site-${i}.c"
      fi
      tst="$(transform_status "$tlog")"
      if [[ "$tst" == "PASS" ]]; then tpass=$((tpass + 1)); else tfail=$((tfail + 1)); fi
      vst="SKIP"
      if [[ "$VERIFY_MUTANTS" != "1" ]]; then
        vst="SKIPPED"
      elif [[ "$tst" != "PASS" || ! -f "$dest/site-${i}.c" ]]; then
        vst="FAIL"; vfail=$((vfail + 1))
      else
        log "  $base  verify site=$i"
        run_verify "$dest/site-${i}.c" "$vlog"
        vst="$(verify_status "$vlog")"
        if [[ "$vst" == "PASS" ]]; then vpass=$((vpass + 1)); else vfail=$((vfail + 1)); fi
      fi
      vd="$(verdict "${ORIG_STATUS[$base]}" "$tst" "$vst" "$n")"
      echo "$base	$i	${ORIG_STATUS[$base]}	$tst	$vst	$vd" >> "$verdicts"
      log "  $vd  $base site=$i"
    done
    echo "$base	${ORIG_STATUS[$base]}	$n	$tpass	$tfail	$vpass	$vfail" >> "$summary"
    [[ -n "$disc_dump" ]] && rm -f "$disc_dump"
  done
  echo
  column -t -s $'\t' "$summary" 2>/dev/null || cat "$summary"
  echo
  column -t -s $'\t' "$verdicts" 2>/dev/null || cat "$verdicts"
}

run_multiple() {
  local src base dest tlog vlog n tst vst vd sv first_tf="${TRANSFORM%%,*}"
  [[ "$TRANSFORM" == "$first_tf" ]] || die "multiple takes one transform; use --mode chain for a list"
  sv="$(site_var_for "$first_tf")"
  [[ -n "$sv" ]] || die "unknown transform: $TRANSFORM"
  local tdir="$OUT/multiple/$first_tf/repeat-$REPEAT"
  local summary="$OUT/multiple/summary.tsv"
  mkdir -p "$tdir" "$OUT/multiple"
  if [[ ! -f "$summary" ]]; then
    echo "transform	repeat	program	transform_status	verify	candidates_round1	verdict" > "$summary"
  fi
  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    dest="$tdir/$base"
    mkdir -p "$dest"
    tlog="$dest/transform.log"
    vlog="$dest/verify.log"
    log "  $base  transform x$REPEAT"
    clear_robustness_env
    export ROBUSTNESS_TRANSFORM="$first_tf"
    if [[ "$SITES" != "all" ]]; then export "$sv=$SITES"; fi
    run_transform "$src" "$tlog" --robustness-repeat "$REPEAT"
    copy_dump "$dest/repeat-${REPEAT}.c"
    n="$(candidate_count "$tlog")"
    tst="$(transform_status "$tlog")"
    vst="SKIP"
    if [[ "$VERIFY_MUTANTS" != "1" ]]; then vst="SKIPPED"
    elif [[ "$tst" == "PASS" && -f "$dest/repeat-${REPEAT}.c" ]]; then
      run_verify "$dest/repeat-${REPEAT}.c" "$vlog"
      vst="$(verify_status "$vlog")"
    else vst="FAIL"
    fi
    vd="$(verdict "${ORIG_STATUS[$base]}" "$tst" "$vst" "$n")"
    echo "$first_tf	$REPEAT	$base	$tst	$vst	$n	$vd" >> "$summary"
    log "  $vd  $base"
  done
  echo
  column -t -s $'\t' "$summary" 2>/dev/null || cat "$summary"
}

run_mixed() {
  local src base dest tlog vlog tst vst vd trace extra
  local tdir="$OUT/spin/repeat-$REPEAT"
  local summary="$OUT/spin/summary.tsv"
  mkdir -p "$tdir"
  echo "program	transform_status	verify	seed	repeat	trace	verdict" > "$summary"
  extra=(--robustness-spin --robustness-repeat "$REPEAT")
  if [[ -n "$SEED" ]]; then extra+=(--robustness-seed "$SEED"); fi
  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    dest="$tdir/$base"
    mkdir -p "$dest"
    tlog="$dest/transform.log"
    vlog="$dest/verify.log"
    log "  $base  spin x$REPEAT"
    clear_robustness_env
    if [[ "$TRANSFORM_SET" == "1" && -n "$TRANSFORM" ]]; then
      export ROBUSTNESS_TRANSFORM="$TRANSFORM"
    fi
    run_transform "$src" "$tlog" "${extra[@]}"
    copy_dump "$dest/spin-${REPEAT}.c"
    tst="$(transform_status "$tlog")"
    trace="$(spin_trace "$tlog")"
    vst="SKIP"
    if [[ "$VERIFY_MUTANTS" != "1" ]]; then vst="SKIPPED"
    elif [[ "$tst" == "PASS" && -f "$dest/spin-${REPEAT}.c" ]]; then
      run_verify "$dest/spin-${REPEAT}.c" "$vlog"
      vst="$(verify_status "$vlog")"
    else vst="FAIL"
    fi
    vd="$(verdict "${ORIG_STATUS[$base]}" "$tst" "$vst" "1")"
    echo "$base	$tst	$vst	${SEED:-auto}	$REPEAT	$trace	$vd" >> "$summary"
    log "  $vd  $base"
  done
  echo
  column -t -s $'\t' "$summary" 2>/dev/null || cat "$summary"
}

run_chain() {
  local src base dest tlog vlog tst vst vd n names nnames slug sv
  IFS=',' read -r -a names <<< "$TRANSFORM"
  nnames=${#names[@]}
  [[ "$nnames" -ge 1 ]] || die "chain needs a comma-separated --transform list"
  if [[ "$REPEAT" -eq 1 && "$nnames" -gt 1 ]]; then REPEAT="$nnames"; fi
  slug="$(echo "$TRANSFORM" | tr ',' '_' | tr -s '_')"
  local tdir="$OUT/chain/$slug/repeat-$REPEAT"
  local summary="$OUT/chain/summary.tsv"
  mkdir -p "$tdir" "$OUT/chain"
  if [[ ! -f "$summary" ]]; then
    echo "chain	repeat	program	transform_status	verify	candidates_round1	verdict" > "$summary"
  fi
  for src in "${FILES[@]}"; do
    base="$(basename "$src" .c)"
    dest="$tdir/$base"
    mkdir -p "$dest"
    tlog="$dest/transform.log"
    vlog="$dest/verify.log"
    log "  $base  chain x$REPEAT"
    clear_robustness_env
    export ROBUSTNESS_TRANSFORM="$TRANSFORM"
    if [[ "$SITES" != "all" ]]; then
      sv="$(site_var_for "${names[0]}")"
      [[ -n "$sv" ]] && export "$sv=$SITES"
    fi
    run_transform "$src" "$tlog" --robustness-repeat "$REPEAT"
    copy_dump "$dest/chain-${REPEAT}.c"
    n="$(candidate_count "$tlog")"
    tst="$(transform_status "$tlog")"
    vst="SKIP"
    if [[ "$VERIFY_MUTANTS" != "1" ]]; then vst="SKIPPED"
    elif [[ "$tst" == "PASS" && -f "$dest/chain-${REPEAT}.c" ]]; then
      run_verify "$dest/chain-${REPEAT}.c" "$vlog"
      vst="$(verify_status "$vlog")"
    else vst="FAIL"
    fi
    vd="$(verdict "${ORIG_STATUS[$base]}" "$tst" "$vst" "1")"
    echo "$TRANSFORM	$REPEAT	$base	$tst	$vst	$n	$vd" >> "$summary"
    log "  $vd  $base"
  done
  echo
  column -t -s $'\t' "$summary" 2>/dev/null || cat "$summary"
}

case "$MODE" in
  preferred) run_preferred ;;
  multiple)  run_multiple ;;
  mixed)     run_mixed ;;
  chain)     run_chain ;;
esac

log "done  $OUT"
```

### `util/robustness/config.env`

Settings the driver reads. Edit this file, then run the script.

**Old:** this path did not exist.

**Now:**

```bash
# Edit this file, then run:
#   ./util/robustness/run-benchmark.sh
#
# Paths without a leading / are relative to the repository root.
# The script clears leftover ADD_IF_*_SITE and ROBUSTNESS_* variables
# from the terminal. You do not unset them by hand.

# How to transform.
#   preferred   = one TRANSFORM, a separate mutant for every site
#   multiple    = one TRANSFORM, applied REPEAT times on the same tree (one mutant)
#   mixed       = random TRANSFORM + random site each step (one mutant)
#   chain       = TRANSFORM as a fixed comma-separated order (one mutant)
#   verify-only = no transform, only verify the originals
MODE=mixed

# Which rewrite(s). One name for preferred/multiple.
# For mixed or chain, a comma-separated list.
# Names: add-if-zero | add-if-one | for-to-while | deepen-while | to-method
# Mixed uses this list as the random pool. Omit a name to exclude it.
TRANSFORM=add-if-zero,add-if-one,for-to-while,deepen-while

# How many times to apply the rewrite on the same tree (multiple / mixed / chain).
REPEAT=10

# Random seed for mixed. Same seed gives the same random sequence. Empty = VerCors picks one.
SEED=42

# Which sites. Used by preferred (every listed index, or all).
# multiple / chain: if not all, exported as the first transform's *_SITE list.
# mixed ignores this; each spin step picks a random site inside VerCors.
SITES=all

# Folder of .c files (not subfolders).
INPUT=benchmark

# Where results are written.
OUT=robustness-results/post-resolution

# Empty = every .c file in INPUT. Or a subset: 1_absolute,2_swap
PROGRAMS=

# Optional. Empty = find out/vercors/main/runScript.dest/vercors or bin/vct.
# VCT=

# Optional Silicon time limit in seconds. Empty = VerCors default.
# TIMEOUT=

# 1 = verify the original .c files first with ordinary vercors (no --robustness).
# 0 = skip that step. Verdict is then SKIP even if the mutant verifies.
VERIFY_ORIGINALS=1

# 1 = verify each mutant with ordinary vercors (needed for ROBUST / NOT_ROBUST).
# 0 = only write the transformed .c files.
VERIFY_MUTANTS=1

# 1 = verify originals again even if _original-verify/*.verify.log already exists.
# 0 = reuse those logs when present. With STAMP=1 the logs live in the new
# run folder, so reuse usually does not find the previous run.
FORCE_ORIGINAL=0

# 1 = write this run into OUT/run-YYYYMMDD-HHMMSS so older results are not overwritten.
# 0 = write directly into OUT.
STAMP=1

# 1 = print the plan only, do not run VerCors. 0 = actually run.
DRY_RUN=0
```

## B. Removed files and flags

These paths or flags existed on this branch and were deleted. They are not
on `origin/dev` either, except that `Transformation.scala` / `Options.scala`
existed and the robustness inject was later taken out.

**Now:** none of the files in this section exist. `--dev-add-if-zero`,
`--dev-add-if-one`, and `--trace-tree-in` are not in `Options.scala`.
`Transformation.scala` does not prepend AddIfZero / AddIfOne. AddIfZero and
AddIfOne run only from `--robustness`.

### `src/rewrite/vct/rewrite/CSourceRewriter.scala` (deleted)

Base rewriter used when AddIfOne still rewrote C source text. AddIfOne now extends `Rewriter` like AddIfZero.

**Old (last committed text):**

```scala
package vct.col.rewrite

import scala.reflect.ClassTag

import vct.col.ast.Declaration
import vct.col.ref.{Ref, UnresolvedRef}

trait CSourceRewriterBuilder extends RewriterBuilder {
  override def apply[Pre <: Generation](): CSourceRewriter[Pre]
}

/**
 * Base rewriter for transformations that run directly on the
 * parsed C COL tree, before Resolution.
 *
 * At this point references may still be unresolved.
 * This preserves them in exactly the same way as LangTypesToCol.
 */
abstract class CSourceRewriter[Pre <: Generation]
  extends Rewriter[Pre] {

  override def porcelainRefSucc[
    RefDecl <: Declaration[Rewritten[Pre]]
  ](
     ref: Ref[Pre, _]
   )(
     implicit tag: ClassTag[RefDecl]
   ): Option[Ref[Rewritten[Pre], RefDecl]] =
    ref match {

      case unresolved: UnresolvedRef[_, _]
        if !unresolved.isResolved =>
        Some(
          new UnresolvedRef[Post, RefDecl](
            unresolved.name
          )
        )

      case _ =>
        None
    }

  override def porcelainRefSeqSucc[
    RefDecl <: Declaration[Rewritten[Pre]]
  ](
     refs: Seq[Ref[Pre, _]]
   )(
     implicit tag: ClassTag[RefDecl]
   ): Option[Seq[Ref[Rewritten[Pre], RefDecl]]] = {

    if (refs.forall(_.isInstanceOf[UnresolvedRef[_, _]])) {
      Some(
        refs
          .map(porcelainRefSucc[RefDecl])
          .map(_.get)
      )
    } else {
      None
    }
  }
}
```

**Now:** file does not exist.

### `src/main/vct/main/stages/CSourceTransformation.scala` (deleted)

Stage that ran `CSourceRewriter` before the statement-only robustness mode. That pipeline is gone.

**Old (last committed text):**

```scala
package vct.main.stages

import hre.stages.Stage

import vct.col.ast.Program
import vct.col.origin.FileSpanningOrigin
import vct.col.print.Ctx
import vct.col.rewrite.{
  CSourceRewriterBuilder,
  Generation,
  Rewritten,
}

import vct.parsers.ParseResult
import vct.parsers.transform.BlameProvider

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

case class CSourceTransformation[
  G <: Generation
](
   blameProvider: BlameProvider,
   pass: CSourceRewriterBuilder,
 ) extends Stage[
  ParseResult[G],
  ParseResult[Rewritten[G]],
] {

  override def friendlyName: String =
    s"C Source Transformation: ${pass.key}"

  override def progressWeight: Int =
    1

  override def run(
                    in: ParseResult[G]
                  ): ParseResult[Rewritten[G]] = {

    implicit val o =
      FileSpanningOrigin

    val program =
      Program[G](
        in.decls
      )(
        blameProvider()
      )

    val rewritten =
      pass
        .apply[G]()
        .dispatch(program)

    /*
     * IMPORTANT:
     *
     * Write the transformed program HERE,
     * before Resolution converts CFunctionDefinition
     * into generic Procedure nodes.
     *
     * CFunctionDefinition's C printer preserves
     * requires / ensures / context_everywhere.
     */
    val outputPath =
      Paths.get(
        "robustness-source.c"
      )

    val writer =
      Files.newBufferedWriter(
        outputPath,
        StandardCharsets.UTF_8,
      )

    try {

      val ctx =
        Ctx(
          syntax = Ctx.C
        ).namesIn(rewritten)

      rewritten.write(writer)(ctx)

    } finally {
      writer.close()
    }

    println(
      "[Robustness] pre-resolution transformed C written to robustness-source.c"
    )

    ParseResult(
      rewritten.declarations,
      in.expectedErrors,
    )
  }
}
```

**Now:** file does not exist.

### `src/rewrite/vct/rewrite/CInsertionSites.scala` (deleted)

Slots for inserting `if (0)` before or after a statement. AddIfZero no longer inserts; it only wraps.

**Old (last committed text):**

```scala
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
```

**Now:** file does not exist.

### `--dev-add-if-zero` / `--dev-add-if-one` (deleted flags)

**Old:** hidden CLI flags set `Options.devAddIfZero` / `devAddIfOne`. Verify
then prepended those rewriters to the ordinary pass list.

From `Options.scala` (commit `70f4cb2d4`):

```scala
opt[Unit]("dev-add-if-one").maybeHidden()
  .action((_, c) => c.copy(devAddIfOne = true))
opt[Unit]("dev-add-if-zero").maybeHidden()
  .action((_, c) => c.copy(devAddIfZero = true))
  .text("Enable the experimental AddIfZero rewrite")

// on case class Options:
devAddIfOne: Boolean = false,
devAddIfZero: Boolean = false,
```

From `Transformation.scala` (same commit):

```scala
devAddIfOne = options.devAddIfOne,
devAddIfZero = options.devAddIfZero,

// SilverTransformation fields:
devAddIfOne: Boolean = false,
devAddIfZero: Boolean = false,

// pass list started with:
Seq[RewriterBuilder](AddIfOne).filter(_ => devAddIfOne) ++
  Seq[RewriterBuilder](AddIfZero).filter(_ => devAddIfZero) ++ Seq(
    CTypeConversions.withArg(checkIntegerBounds, unsetTarget),
    // ... ordinary Verify passes
```

**Now:** those flags and fields are gone. The Verify list does not mention
AddIfZero or AddIfOne.

### `--trace-tree-in` / `outputTree` (deleted flag)

**Old:** `Options.scala` declared the flag and field. Nothing in `Main` or
a stage read `outputTree`.

```scala
opt[Path]("trace-tree-in")
  .action((p, c) => c.copy(outputTree = Some(p))).text(
    "Writes structural AST tree snapshots to a given folder"
  )

// on case class Options:
outputTree: Option[Path] = None,
```

**Now:** the flag and field are gone.

### SemTransforms name aliases (deleted behaviour)

`parseTransform` and the driver no longer accept `if0error`, `add-if1` /
`add_if1`, `for2while`, `FOR2WHILE_SITE`, or `ROBUSTNESS_TRANSFORM=spin` /
`spin_config`. There is no old source file for this; it was extra `case`
arms that were removed. Current names are only
`add-if-zero`, `add-if-one`, `for-to-while`, `deepen-while`, `to-method`
(plus underscore forms of those same words).

## C. Modified existing files

These paths existed on `origin/dev`. Only the robustness-related hunks are shown. Isar/LSP deletions on this branch are omitted.

## 1. Hook the mode into VerCors

### `src/main/vct/options/types/Mode.scala`

**Before:** there was no robustness mode.

```scala
case object Verify extends Mode
case object VeyMont extends Mode
```

**After:** `Mode.Robustness` exists so `--robustness` has a mode value.

```scala
case object Verify extends Mode
case object Robustness extends Mode
case object VeyMont extends Mode
```

**Before:** CLI could not name this pipeline.  
**Now:** `Options.mode` can be `Mode.Robustness`.

---

### `src/main/vct/main/Main.scala`

**Before:** `runMode` had no robustness arm. Import list had no
`Robustness`.

```scala
def runMode(mode: Mode, options: Options): Int =
  mode match {
    case Mode.Verify => Verify.runOptions(options)
    case Mode.HelpVerifyPasses =>
      // ...
```

**After:** `--robustness` dispatches to `Robustness.runOptions`.

```scala
import vct.main.modes.{CFG, Compile, Patcher, Robustness, VeSUV, Verify, VeyMont}

def runMode(mode: Mode, options: Options): Int =
  mode match {
    case Mode.Verify => Verify.runOptions(options)
    case Mode.Robustness => Robustness.runOptions(options)
    case Mode.HelpVerifyPasses =>
      // ...
```

**Before:** that flag could not start a pipeline.  
**Now:** it runs parse → resolve → selected rewrite(s) → C dump. It still
does not run Silicon.

---

### `src/main/vct/options/Options.scala`

**Before:** no robustness flags and no fields.

**After (parser):**

```scala
opt[Unit]("robustness")
  .action((_, c) => c.copy(mode = Mode.Robustness))
  .text("Rewrite after resolve and write a C dump; does not run Silicon"),

opt[Int]("robustness-repeat")
  .action((amount, c) => c.copy(robustnessRepeat = amount)),

opt[Unit]("robustness-spin")
  .action((_, c) => c.copy(robustnessSpin = true)),

opt[Long]("robustness-seed")
  .action((seed, c) => c.copy(robustnessSeed = Some(seed))),
```

**After (fields on `Options`):**

```scala
robustnessRepeat: Int = 1,
robustnessSpin: Boolean = false,
robustnessSeed: Option[Long] = None,
```

**Before:** only ordinary Verify (and the other existing modes).  
**Now:** repeat, spin, and seed are parsed here and read in
`Robustness.scala`. There is still no per-transform flag; the rewrite name
is `ROBUSTNESS_TRANSFORM`.

---

## 2. Share the existing for → while algorithm

### `src/rewrite/vct/rewrite/ForLoopToWhileLoop.scala`

**Before:** the conversion lived only inside the Verify rewriter’s
`dispatch`. Robustness could not call it for one selected `for`.

```scala
override def dispatch(stat: Statement[Pre]): Statement[Post] =
  stat match {
    case Loop(Block(Nil), cond, Block(Nil), contract, body) =>
      rewriteDefault(stat)
    case loop @ Loop(init, cond, update, contract, body) =>
      implicit val o: Origin = stat.o
      Block(Seq(
        dispatch(init),
        Loop(
          Block(Nil),
          dispatch(cond),
          Block(Nil),
          dispatch(contract),
          Block(Seq(dispatch(body), dispatch(update))),
        ),
      ))
    case other => rewriteDefault(other)
  }
```

**After:** the same algorithm is `rewriteLoop`. The Verify pass still
calls it for every `Loop`. The robustness for-to-while rewriter calls it
for one selected `Loop`.

```scala
def rewriteLoop[Pre <: Generation, Post <: Generation](
    loop: Loop[Pre],
    rewriteDefaultLoop: Loop[Pre] => Statement[Post],
    dispatchStatement: Statement[Pre] => Statement[Post],
    dispatchExpr: Expr[Pre] => Expr[Post],
    dispatchContract: LoopContract[Pre] => LoopContract[Post],
): Statement[Post] =
  loop match {
    case Loop(Block(Nil), _, Block(Nil), _, _) =>
      rewriteDefaultLoop(loop)
    case Loop(init, cond, update, contract, body) =>
      implicit val o: Origin = loop.o
      Block[Post](Seq(
        dispatchStatement(init),
        Loop[Post](
          Block[Post](Nil),
          dispatchExpr(cond),
          Block[Post](Nil),
          dispatchContract(contract),
          Block[Post](Seq(
            dispatchStatement(body),
            dispatchStatement(update),
          )),
        ),
      ))
  }

override def dispatch(stat: Statement[Pre]): Statement[Post] =
  stat match {
    case loop: Loop[Pre] =>
      ForLoopToWhileLoop.rewriteLoop(
        loop,
        (loop: Loop[Pre]) => loop.rewriteDefault(),
        (stat: Statement[Pre]) => dispatch(stat),
        (expr: Expr[Pre]) => dispatch(expr),
        (contract: LoopContract[Pre]) => dispatch(contract),
      )
    case other => other.rewriteDefault()
  }
```

**Before:** one copy of the algorithm, Verify-only.  
**Now:** one copy, two callers. Verify still rewrites every `for`.
Behaviour of that pass is the same algorithm extracted, not a new
conversion.

---

## 3. Hide generated sizeof helpers in C dumps

The helper is still created in the AST. Pointer-size encoding uses it.
C-family **Program** print omits the declaration.

### `src/col/vct/col/origin/Origin.scala`

**Before:** no marker type.

**After (added):**

```scala
case object GeneratedSizeOfHelper extends OriginContent
```

**Before:** a sizeof helper could not be distinguished from an ordinary
function by origin content.  
**Now:** LangCToCol can tag the helper.

---

### `src/rewrite/vct/rewrite/lang/LangCToCol.scala`

**Before:** sizeof helpers were declared with a name only.

```scala
)(o.where(name = s"sizeOf_$t"))
```

**After:** the same declaration is tagged. There are two call sites
(array sizeof and the other sizeof). Both got the same tag.

```scala
)(o.where(name = s"sizeOf_$t")
  .withContent(GeneratedSizeOfHelper))
```

**Before:** dump of a resolved C program printed `pure int sizeOf_...()`.  
**Now:** the node is still in the AST; Program print can drop it (next
file).

---

### `src/col/vct/col/ast/declaration/global/FunctionImpl.scala`

**Before:** no `isGeneratedSizeOfHelper`. `layout` / `layoutSpec`
unchanged (except an unrelated `Ctx.Isar` arm removed on this branch).

**After (added):**

```scala
def isGeneratedSizeOfHelper: Boolean =
  o.find[vct.col.origin.GeneratedSizeOfHelper.type].nonEmpty ||
    (body.isEmpty &&
      o.getPreferredName.exists { name =>
        val snake = name.snake.toLowerCase
        snake.startsWith("sizeof_") || snake.startsWith("size_of_")
      })
```

**Before:** Program print had no predicate to filter on.  
**Now:** origin marker is the primary test; the name fallback covers a
helper printed without the marker.

---

### `src/col/vct/col/ast/node/ProgramImpl.scala`

**Before:** every declaration was printed.

```scala
override def layout(implicit ctx: Ctx): Doc = {
  (if (ctx.syntax == Ctx.Java)
     (Text("import java.util.concurrent.locks.Lock;") <+/>
       "import java.util.concurrent.locks.ReentrantLock;" <+/>
       "import java.util.concurrent.locks.Condition;" <> Line)
   else
     Empty) <> Doc.stack2(declarations)
}
```

**After:** C-family print drops sizeof helpers first.

```scala
override def layout(implicit ctx: Ctx): Doc = {
  val decls =
    ctx.syntax match {
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP =>
        declarations.filterNot {
          case f: vct.col.ast.Function[_] => f.isGeneratedSizeOfHelper
          case _ => false
        }
      case _ =>
        declarations
    }

  (if (ctx.syntax == Ctx.Java)
     (Text("import java.util.concurrent.locks.Lock;") <+/>
       "import java.util.concurrent.locks.ReentrantLock;" <+/>
       "import java.util.concurrent.locks.Condition;" <> Line)
   else
     Empty) <> Doc.stack2(decls)
}
```

**Before:** a C dump included `sizeof_*` functions the C parser does not
accept as source.  
**Now:** those declarations are omitted for C-family syntax only. PVL,
Silver, and Java still print the full list.

---

## 4. Shared C pretty-print (dump must parse as C)

### `src/col/vct/col/print/Ctx.scala`

**Before:** `name` printed the namer string as-is (then optional PVL/spec
backticks).

```scala
def name(decl: Declaration[_]): String = {
  val name = names.getOrElse(
    decl,
    s"${decl.o.getPreferredNameOrElse().ucamel}_${decl.hashCode()}",
  )
  if ((inSpec || syntax == Ctx.PVL) && Keywords.SPEC.contains(name))
    "`" + name + "`"
  else
    name
}
```

**After:** C-family names are forced to `[A-Za-z_][A-Za-z0-9_]*`.

```scala
private def usesCIdentifiers: Boolean =
  syntax match {
    case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL => true
    case _ => false
  }

private def cIdentifier(name: String): String = {
  val sanitized =
    name.map {
      case c if c.isLetterOrDigit || c == '_' => c
      case _ => '_'
    }.mkString
  if (sanitized.isEmpty || sanitized.head.isDigit)
    "_" + sanitized
  else
    sanitized
}

def name(decl: Declaration[_]): String = {
  val name = names.getOrElse(
    decl,
    s"${decl.o.getPreferredNameOrElse().ucamel}_${decl.hashCode()}",
  )
  val printableName =
    if (usesCIdentifiers)
      cIdentifier(name)
    else
      name
  if ((inSpec || syntax == Ctx.PVL) && Keywords.SPEC.contains(printableName))
    "`" + printableName + "`"
  else
    printableName
}
```

**Before:** a type-derived name could contain characters that are not C
identifiers.  
**Now:** C dumps use a sanitized identifier. Other syntaxes are
unchanged.

---

### `src/col/vct/col/ast/type/TBoolImpl.scala`

**Before:** C family all printed `bool`.

```scala
case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => Text("bool")
```

**After:**

```scala
case Ctx.C => Text("_Bool")
case Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => Text("bool")
```

**Before:** a C99 dump could contain `bool`, which is not a C99 keyword.  
**Now:** `Ctx.C` prints `_Bool`. Cuda / OpenCL / CPP still print `bool`.

---

### `src/col/vct/col/ast/type/TArrayImpl.scala`

**Before:** only `layout` (`element[]`). No C split declarator.

**After (added):**

```scala
override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) =
  ctx.syntax match {
    case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
      val (spec, decl) = element.layoutSplitDeclarator
      (spec, decl <> "*")
    case _ => (layout, Empty)
  }
```

**Before:** a C parameter of array type could print in COL array form.  
**Now:** C-family split layout is pointer form `T*`.

---

### Pointer-array types

Same addition on all four: C-family `layoutSplitDeclarator` prints
`element` then `*`, not COL `T[]`.

Files:

- `TPointerArrayImpl.scala`
- `TConstPointerArrayImpl.scala` (specifier is `const` plus element)
- `TNonNullPointerArrayImpl.scala` (non-null is not C syntax; C dump is
  still `T*`)
- `TNonNullConstPointerArrayImpl.scala` (C dump is `const T*`)

**Before** (`TPointerArrayImpl` is representative): no
`layoutSplitDeclarator`; `layout` used `[]`.

```scala
override def layout(implicit ctx: Ctx): Doc =
  dimensions.foldLeft[Doc](
    unique.map(u => Text(s"unique<$u>") <+> element).getOrElse(element.show)
  ) {
    case (l, Some(r)) => l <> "[" <> r <> "]"
    case (l, None) => l <> "[]"
  }
```

**After:**

```scala
override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) =
  ctx.syntax match {
    case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
      val (spec, decl) = element.layoutSplitDeclarator
      (spec, decl <> "*")
    case _ => (layout, Empty)
  }
```

`layout` (non-split) is unchanged in meaning; it was only refactored to a
helper. PVL/Silver still see `[]` / `NonNull` text through `layout`, not
through the C split path.

**Before:** resolved pointer arrays in a C dump could print as COL arrays.  
**Now:** C declarator form is `T*`.

---

### `src/col/vct/col/ast/type/TClassImpl.scala`

**Before:** every syntax printed the class name, with optional `<typeArgs>`.

```scala
override def layout(implicit ctx: Ctx): Doc =
  Group(
    Text(ctx.name(cls)) <>
      (if (typeArgs.nonEmpty)
         Text("<") <> Doc.args(typeArgs) <> ">"
       else
         Empty)
  )
```

**After:** C-family prints a by-value class (resolved C struct) as
`struct T`. Other syntaxes keep the old layout.

```scala
private def layoutCName(implicit ctx: Ctx): Doc =
  cls.decl match {
    case _: ByValueClass[G] =>
      Text("struct") <+> ctx.name(cls)
    case _ => Text(ctx.name(cls))
  }

override def layout(implicit ctx: Ctx): Doc =
  ctx.syntax match {
    case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => Group(layoutCName)
    case _ =>
      Group(
        Text(ctx.name(cls)) <>
          (if (typeArgs.nonEmpty)
             Text("<") <> Doc.args(typeArgs) <> ">"
           else
             Empty)
      )
  }

override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) =
  ctx.syntax match {
    case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => (layoutCName, Empty)
    case _ => (layout, Empty)
  }
```

**Before:** a resolved struct type could print as a bare class name.  
**Now:** C dumps use `struct T`.

---

### `src/col/vct/col/ast/declaration/global/ClassImpl.scala`

**Before:** C syntax fell through to PVL class layout.

```scala
override def layout(implicit ctx: Ctx): Doc =
  ctx.syntax match {
    case Ctx.Java => layoutJava
    case _ => layoutPvl
  }
```

**After:**

```scala
def layoutC(implicit ctx: Ctx): Doc =
  this match {
    case _: ByValueClass[G] =>
      Text("struct") <+> ctx.name(this) <+> "{" <>> Doc.stack2(decls) <+/>
        "};"
    case _ => layoutPvl
  }

override def layout(implicit ctx: Ctx): Doc =
  ctx.syntax match {
    case Ctx.Java => layoutJava
    case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => layoutC
    case _ => layoutPvl
  }
```

**Before:** a resolved C struct definition printed as `class ...`.  
**Now:** by-value classes print as `struct T { ... };` under C-family
syntax.

---

### `src/col/vct/col/ast/declaration/global/ProcedureImpl.scala`

**Before:** C layout was the type and name only; the contract was omitted.

```scala
def layoutC(implicit ctx: Ctx): Doc = {
  val (spec, decl) = returnType.layoutSplitDeclarator
  Group(spec <+> decl <> ctx.name(this) <> "(" <> Doc.args(args) <> ")") <>
    (if (body.nonEmpty)
       Text(" ") <> body.get.layoutAsBlock
     else
       Text(";"))
}
```

**After:** contract is stacked above the C signature; `inline` is printed
when set.

```scala
def layoutC(implicit ctx: Ctx): Doc = {
  val (spec, decl) = returnType.layoutSplitDeclarator
  val prefix = if (inline) Text("inline") <> " " else Empty
  Doc.stack(Seq(
    contract,
    Group(
      prefix <> spec <+> decl <> ctx.name(this) <> "(" <>
        Doc.args(args) <> ")"
    ) <>
      (if (body.nonEmpty)
         Text(" ") <> body.get.layoutAsBlock
       else
         Text(";")),
  ))
}
```

**Before:** a resolved C function dumped as a `Procedure` lost its
contract in `Ctx.C`.  
**Now:** the contract is in the C dump.

---

### `src/col/vct/col/ast/statement/composite/LoopImpl.scala`

Two related edits.

**For-init (`simpleControlElements`).**  
**Before:** a resolved `int i = e` inside a `Scope`/`Block` did not match
and `layoutControl` fell back to printing a nested block. C
`for ( { int i = e; } ; ...)` is invalid.

**After (added cases, all print `local = value`):**

```scala
case Scope(_, Block(Seq(LocalDecl(local), Assign(Local(Ref(v)), value))))
    if local == v =>
  Some(local.show <+> "=" <+> value)

case Scope(_, Block(Seq(LocalDecl(local), AssignInitial(Local(Ref(v)), value))))
    if local == v =>
  Some(local.show <+> "=" <+> value)

case Scope(Seq(local), Block(Seq(Assign(Local(Ref(v)), value))))
    if local == v =>
  Some(local.show <+> "=" <+> value)

case Scope(Seq(local), Block(Seq(AssignInitial(Local(Ref(v)), value))))
    if local == v =>
  Some(local.show <+> "=" <+> value)

case Block(Seq(LocalDecl(local), Assign(Local(Ref(v)), value)))
    if local == v =>
  Some(local.show <+> "=" <+> value)

case Block(Seq(LocalDecl(local), AssignInitial(Local(Ref(v)), value)))
    if local == v =>
  Some(local.show <+> "=" <+> value)
```

**Loop body braces.**  
**Before:** while/for always used `body.layoutAsBlock`.

**After:** C-family uses `layoutAsBlock` only when the body is already
`Block` or `Scope`; otherwise `body.show`. That avoids a second pair of
braces around a body that is not a block.

```scala
private def layoutGenericBody(implicit ctx: Ctx): Doc =
  ctx.syntax match {
    case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP =>
      body match {
        case Block(_) | Scope(_, _) => body.layoutAsBlock
        case _ => body.show
      }
    case _ => body.layoutAsBlock
  }
```

`layoutGenericWhile` and `layoutGenericFor` call `layoutGenericBody`
instead of `body.layoutAsBlock`.

**Before:** resolved for-inits and some bodies printed as illegal C.  
**Now:** for-init prints as one declarator `int i = e`; C body braces
follow the node shape.

---

### `src/col/vct/col/ast/expr/literal/constant/BooleanValueImpl.scala`

**Before:** every syntax used the `true`/`false` letter puzzle (`layoutDefault`).

**After:** executable C (`Ctx.C` and not `inSpec`) prints `1` / `0`.
Specifications and other languages still print `true` / `false`.

```scala
override def layout(implicit ctx: Ctx): Doc =
  ctx.syntax match {
    case Ctx.C if !ctx.inSpec =>
      Text(if (value) "1" else "0")
    case _ =>
      Text(
        orderedLayoutFixture.collect {
          case e @ SharedLayoutElement(_) => e
          case e @ DedicatedLayoutElement(r, _)
              if r.toString == value.toString =>
            e
        }.map(_.textualData).mkString("")
      )
  }
```

**Before:** a C dump of a condition could contain `true`, which is not
C99.  
**Now:** executable C uses `1`/`0`; contracts keep `true`/`false`.

---

### `src/col/vct/col/ast/expr/literal/constant/NullImpl.scala`

**Before:**

```scala
override def layout(implicit ctx: Ctx): Doc = Text("null")
```

**After:**

```scala
override def layout(implicit ctx: Ctx): Doc =
  ctx.syntax match {
    case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL => Text("NULL")
    case _ => Text("null")
  }
```

**Before:** C dumps contained `null`.  
**Now:** C-family prints `NULL`. Other syntaxes still print `null`.

---

## D. Not source (not dumped here)

| Path | Why |
|---|---|
| `ROBUSTNESS-FILES.md`, `ROBUSTNESS.md`, this file | Documentation. |
| `robustness-results/post-resolution/` | Driver output, not implementation. |
| `robustness-transformed.c` | Last dump in the working directory. |
