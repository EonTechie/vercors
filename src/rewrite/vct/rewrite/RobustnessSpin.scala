package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

import vct.col.ast.Program

/** One SemTransforms-style spin step: pick a transform kind, then a site.
  *
  * Mirrors `Transformer.transform` in FlorianDyck/semtransforms: weighted
  * choice of kind, skip a kind with no sites and retry, abort remaining
  * repetitions if nothing in the pool applies, then uniform choice among
  * that kind's current candidates. The chosen rewriter is the existing
  * AddIfZero / AddIfOne / RobustnessForLoopToWhileLoop pass.
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
