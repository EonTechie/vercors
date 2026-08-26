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

  private def selectedSiteIndex(candidates: Seq[_]): Int = {
    val envName =
      if (sys.env.contains("FOR_TO_WHILE_SITE"))
        "FOR_TO_WHILE_SITE"
      else if (sys.env.contains("FOR2WHILE_SITE"))
        "FOR2WHILE_SITE"
      else
        "FOR_TO_WHILE_SITE"

    RobustnessSiteSelection.nextIndex(
      envName,
      candidates.size,
      required = true,
    ).get
  }

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
          required = false,
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
