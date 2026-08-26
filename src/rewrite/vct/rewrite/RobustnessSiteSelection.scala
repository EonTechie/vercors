package vct.col.rewrite

import java.util.concurrent.atomic.AtomicInteger

/** Picks one site index per robustness application.
  *
  * `ADD_IF_ZERO_SITE=0,2,1` with `--robustness-repeat 3` uses 0 then 2 then 1.
  * A single index is reused on later applications.
  */
object RobustnessSiteSelection {
  private val application = new AtomicInteger(0)

  def reset(): Unit = application.set(0)

  def nextIndex(
      envName: String,
      candidateCount: Int,
      required: Boolean,
  ): Option[Int] = {
    val round = application.getAndIncrement()

    if (candidateCount <= 0) { return None }

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

        Some(selected)

      case None if required =>
        throw new IllegalArgumentException(s"$envName must be specified")

      case None => None
    }
  }
}
