package vct.col.rewrite

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Picks one site index per robustness application of a named transform.
  *
  * `ADD_IF_ZERO_SITE=0,2,1` with `--robustness-repeat 3` uses 0 then 2 then 1.
  * A shorter list reuses its last index on later applications.
  *
  * If the env var is unset, each application selects site 0 (deterministic,
  * suitable for SemTransforms-style N-fold repeats). Counters are per env
  * name so mixed transform chains do not share site lists.
  *
  * If an application has no remaining candidates, it is a no-op.
  */
object RobustnessSiteSelection {
  private val applications = new ConcurrentHashMap[String, AtomicInteger]()

  def reset(): Unit = applications.clear()

  def nextIndex(
      envName: String,
      candidateCount: Int,
      required: Boolean,
  ): Option[Int] = {
    val round = applications.computeIfAbsent(
      envName,
      _ => new AtomicInteger(0),
    ).getAndIncrement()

    if (candidateCount <= 0) {
      println(s"[$envName] application ${round + 1}: 0 candidates, skip")
      return None
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

      case None if required =>
        throw new IllegalArgumentException(s"$envName must be specified")

      case None =>
        println(
          s"[$envName] application ${round + 1}: default site 0 / $candidateCount"
        )
        Some(0)
    }
  }
}
