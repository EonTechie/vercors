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
      required = false,
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
