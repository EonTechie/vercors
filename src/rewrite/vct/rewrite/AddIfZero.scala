package vct.col.rewrite

import scala.util.Random

import vct.col.ast._
import RewriteHelpers._
import vct.col.origin.{Origin, PanicBlame}

case object AddIfZero
  extends CSourceRewriterBuilder {

  override def key: String =
    "addIfZero"

  override def desc: String =
    "Wrap a C source statement in the else branch of if (0)."
}

case class AddIfZero[
  Pre <: Generation
]() extends CSourceRewriter[Pre] {

  private var selected:
    Option[CStatementSites.Site[Pre]] =
    None

  private def cInt(
                    value: BigInt,
                    origin: Origin,
                  ): CIntegerValue[Post] = {

    implicit val o: Origin =
      origin

    CIntegerValue[Post](
      value,
      CPrimitiveType[Post](
        Seq(
          CInt[Post]()
        )
      )
    )
  }

  /*
   * CToCol represents a real C compound:
   *
   *     { ... }
   *
   * as:
   *
   *     Scope(Nil, Block(...))
   *
   * Use the same shape for generated braces.
   */
  private def asSourceCompound(
                                stat: Statement[Post],
                                origin: Origin,
                              ): Statement[Post] =
    stat match {

      // Already a source compound.
      case scope @ Scope(
        Nil,
        Block(_)
      ) =>
        scope

        // Any non-compound source statement must be put inside
        // braces, matching SemTransformers add_necessities.
      case other =>
        Scope[Post](
          Nil,
          Block[Post](
            Seq(other)
          )(origin)
        )(origin)
    }

  private def deadBranch(
                          origin: Origin
                        ): Statement[Post] = {

    val assertion =
      Assert[Post](
        BooleanValue[Post](
          false
        )(origin)
      )(
        PanicBlame(
          "AddIfZero: dead branch became reachable"
        )
      )(origin)

    asSourceCompound(
      assertion,
      origin,
    )
  }

  override def dispatch(
                         program: Program[Pre]
                       ): Program[Post] = {

    val candidates =
      CStatementSites.collect(program)

    println(
      s"[AddIfZero] candidate count = ${candidates.size}"
    )

    candidates.zipWithIndex.foreach {
      case (site, index) =>

        println(
          s"[AddIfZero] candidate $index" +
            s" | function=${site.functionName}" +
            s" | role=${site.role.label}" +
            s" | path=${site.path}" +
            s" | kind=${site.description}"
        )
    }

    selected =
      if (candidates.nonEmpty) {

        val selectedIndex =
          sys.env
            .get("ADD_IF_ZERO_SITE")
            .map(_.toInt)
            .getOrElse(
              Random.nextInt(candidates.size)
            )

        if (
          selectedIndex < 0 ||
            selectedIndex >= candidates.size
        ) {
          throw new IllegalArgumentException(
            s"Invalid AddIfZero site index: $selectedIndex. " +
              s"Valid range: 0..${candidates.size - 1}"
          )
        }

        val site =
          candidates(selectedIndex)

        println(
          s"[AddIfZero] SELECTED INDEX = $selectedIndex"
        )

        println(
          s"[AddIfZero] SELECTED" +
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

        val rewrittenOriginal =
          stat.rewriteDefault()

        Branch[Post](
          Seq(
            (
              cInt(
                BigInt(0),
                stat.o,
              ),
              deadBranch(stat.o),
            ),
            (
              BooleanValue[Post](
                true
              )(stat.o),
              asSourceCompound(
                rewrittenOriginal,
                stat.o,
              ),
            ),
          )
        )(stat.o)

      case _ =>
        stat.rewriteDefault()
    }
  }
}