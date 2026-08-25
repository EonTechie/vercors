package vct.col.rewrite

import vct.col.ast._
import vct.col.ast.RewriteHelpers._
import vct.col.origin.{Origin, PanicBlame}

case object AddIfZero extends RewriterBuilder {
  override def key: String = "addIfZero"

  override def desc: String =
    "Insert a dead if (false) assertion at a C statement-list site."
}

case class AddIfZero[Pre <: Generation]() extends Rewriter[Pre] {
  private var selected: Option[CInsertionSites.Site[Pre]] = None

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

  private def emptyBranch(origin: Origin): Statement[Post] =
    Scope[Post](Nil, Block[Post](Nil)(origin))(origin)

  private def insertedStatement(origin: Origin): Statement[Post] =
    Branch[Post](Seq(
      (bool(value = false, origin), deadBranch(origin)),
      (bool(value = true, origin), emptyBranch(origin)),
    ))(origin)

  override def dispatch(program: Program[Pre]): Program[Post] = {
    // Adjusted for robustness mode: collect SemTransformers-style zero-length
    // insertion slots from the resolved AST and select one by index.
    val candidates = CInsertionSites.collect(program)

    println(s"[AddIfZero] candidate count = ${candidates.size}")

    candidates.zipWithIndex.foreach { case (site, index) =>
      println(
        s"[AddIfZero] candidate $index" + s" | function=${site.functionName}" +
          s" | path=${site.path}" + s" | kind=${site.description}"
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
          s" | path=${site.path}" + s" | kind=${site.description}"
      )
    }

    super.dispatch(program)
  }

  override def dispatch(stat: Statement[Pre]): Statement[Post] =
    stat match {
      case block @ Block(statements)
          if selected.exists(site => site.container eq block) =>
        val site = selected.get
        val rewritten =
          statements.take(site.index).map(dispatch) ++
            Seq(insertedStatement(block.o)) ++
            statements.drop(site.index).map(dispatch)

        Block[Post](rewritten)(block.o)

      case _ => stat.rewriteDefault()
    }
}
