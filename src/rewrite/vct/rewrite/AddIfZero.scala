package vct.col.rewrite

import vct.col.ast._
import vct.col.ast.RewriteHelpers._
import vct.col.origin.{Origin, PanicBlame}

case object AddIfZero extends RewriterBuilder {
  override def key: String = "addIfZero"

  override def desc: String =
    "Wrap a statement in the else branch of if (false)."
}

case class AddIfZero[Pre <: Generation]() extends Rewriter[Pre] {
  private var selected: Option[CStatementSites.Site[Pre]] = None

  private def bool(value: Boolean, origin: Origin): BooleanValue[Post] =
    BooleanValue[Post](value)(origin)

  private def asCompound(
      stat: Statement[Post],
      origin: Origin,
  ): Statement[Post] =
    stat match {
      case scope @ Scope(Nil, Block(_)) => scope
      case other => Scope[Post](Nil, Block[Post](Seq(other))(origin))(origin)
    }

  private def deadBranch(origin: Origin): Statement[Post] =
    asCompound(
      Assert[Post](BooleanValue[Post](false)(origin))(PanicBlame(
        "AddIfZero: dead branch became reachable"
      ))(origin),
      origin,
    )

  private def transformedTarget(stat: Statement[Pre]): Statement[Post] =
    Branch[Post](Seq(
      (bool(value = false, stat.o), deadBranch(stat.o)),
      (bool(value = true, stat.o), asCompound(stat.rewriteDefault(), stat.o)),
    ))(stat.o)

  override def dispatch(program: Program[Pre]): Program[Post] = {
    // Adjusted for robustness mode: collect candidates from the resolved AST
    // and select one site by index for reproducible mutant generation.
    val candidates = CStatementSites.collect(program)

    println(s"[AddIfZero] candidate count = ${candidates.size}")

    candidates.zipWithIndex.foreach { case (site, index) =>
      println(
        s"[AddIfZero] candidate $index" + s" | function=${site.functionName}" +
          s" | role=${site.role.label}" + s" | path=${site.path}" +
          s" | kind=${site.description}"
      )
    }

    if (candidates.isEmpty) { selected = None }
    else {
      val selectedIndex = sys.env.get("ADD_IF_ZERO_SITE").map(_.toInt)
        .getOrElse(
          throw new IllegalArgumentException(
            "ADD_IF_ZERO_SITE must be specified"
          )
        )

      if (selectedIndex < 0 || selectedIndex >= candidates.size) {
        throw new IllegalArgumentException(
          s"Invalid AddIfZero site index: $selectedIndex. " +
            s"Valid range: 0..${candidates.size - 1}"
        )
      }

      val site = candidates(selectedIndex)
      selected = Some(site)

      println(s"[AddIfZero] SELECTED INDEX = $selectedIndex")
      println(
        s"[AddIfZero] SELECTED" + s" | function=${site.functionName}" +
          s" | role=${site.role.label}" + s" | path=${site.path}" +
          s" | kind=${site.description}"
      )
    }

    super.dispatch(program)
  }

  override def dispatch(stat: Statement[Pre]): Statement[Post] =
    selected match {
      case Some(site) if site.target eq stat => transformedTarget(stat)
      case _ => stat.rewriteDefault()
    }
}
