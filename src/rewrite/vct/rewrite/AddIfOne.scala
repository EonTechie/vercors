package vct.col.rewrite

import vct.col.ast._
import vct.col.ast.RewriteHelpers._
import vct.col.origin.{Origin, PanicBlame}

case object AddIfOne extends CSourceRewriterBuilder {
  override def key: String = "addIfOne"

  override def desc: String =
    "Wrap a C statement as if (1) { original } else { assert false }."
}

case class AddIfOne[Pre <: Generation]() extends CSourceRewriter[Pre] {
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
      required = false,
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
