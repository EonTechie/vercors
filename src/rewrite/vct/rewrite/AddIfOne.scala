package vct.col.rewrite

import scala.util.Random

import vct.col.ast._
import RewriteHelpers._
import vct.col.origin.Origin

case object AddIfOne
  extends CSourceRewriterBuilder {

  override def key: String =
    "addIfOne"

  override def desc: String =
    "Wrap a C source statement in if (1)."
}

case class AddIfOne[
  Pre <: Generation
]() extends CSourceRewriter[Pre] {

  private var selected:
    Option[CStatementSites.Site[Pre]] =
    None

  private def one(
                   origin: Origin
                 ): CIntegerValue[Post] = {

    implicit val o: Origin =
      origin

    CIntegerValue[Post](
      BigInt(1),
      CPrimitiveType[Post](
        Seq(
          CInt[Post]()
        )
      )
    )
  }

  private def asSourceCompound(
                                stat: Statement[Post],
                                origin: Origin,
                              ): Statement[Post] =
    stat match {

      case scope @ Scope(
        Nil,
        Block(_)
      ) =>
        scope

      case other =>
        Scope[Post](
          Nil,
          Block[Post](
            Seq(other)
          )(origin)
        )(origin)
    }

  override def dispatch(
                         program: Program[Pre]
                       ): Program[Post] = {

    val candidates =
      CStatementSites.collect(program)

    println(
      s"[AddIfOne] candidate count = ${candidates.size}"
    )

    candidates.zipWithIndex.foreach {
      case (site, index) =>

        println(
          s"[AddIfOne] candidate $index" +
            s" | function=${site.functionName}" +
            s" | role=${site.role.label}" +
            s" | path=${site.path}" +
            s" | kind=${site.description}"
        )
    }

    selected =
      if (candidates.nonEmpty) {

        val selectedIndex =
          Random.nextInt(
            candidates.size
          )

        val site =
          candidates(selectedIndex)

        println(
          s"[AddIfOne] SELECTED INDEX = $selectedIndex"
        )

        println(
          s"[AddIfOne] SELECTED" +
            s" | function=${site.functionName}" +
            s" | role=${site.role.label}" +
            s" | path=${site.path}" +
            s" | kind=${site.description}"
        )

        Some(site)

      } else {
        None
      }

    program.rewriteDefault()
  }

  override def dispatch(
                         stat: Statement[Pre]
                       ): Statement[Post] = {

    selected match {

      case Some(site)
        if site.target eq stat =>

        Branch[Post](
          Seq(
            (
              one(stat.o),
              asSourceCompound(
                stat.rewriteDefault(),
                stat.o,
              ),
            )
          )
        )(stat.o)

      case _ =>
        stat.rewriteDefault()
    }
  }
}