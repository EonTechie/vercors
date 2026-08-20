package vct.col.rewrite

import scala.reflect.ClassTag

import vct.col.ast.Declaration
import vct.col.ref.{Ref, UnresolvedRef}

trait CSourceRewriterBuilder extends RewriterBuilder {
  override def apply[Pre <: Generation](): CSourceRewriter[Pre]
}

/**
 * Base rewriter for transformations that run directly on the
 * parsed C COL tree, before Resolution.
 *
 * At this point references may still be unresolved.
 * This preserves them in exactly the same way as LangTypesToCol.
 */
abstract class CSourceRewriter[Pre <: Generation]
  extends Rewriter[Pre] {

  override def porcelainRefSucc[
    RefDecl <: Declaration[Rewritten[Pre]]
  ](
     ref: Ref[Pre, _]
   )(
     implicit tag: ClassTag[RefDecl]
   ): Option[Ref[Rewritten[Pre], RefDecl]] =
    ref match {

      case unresolved: UnresolvedRef[_, _]
        if !unresolved.isResolved =>
        Some(
          new UnresolvedRef[Post, RefDecl](
            unresolved.name
          )
        )

      case _ =>
        None
    }

  override def porcelainRefSeqSucc[
    RefDecl <: Declaration[Rewritten[Pre]]
  ](
     refs: Seq[Ref[Pre, _]]
   )(
     implicit tag: ClassTag[RefDecl]
   ): Option[Seq[Ref[Rewritten[Pre], RefDecl]]] = {

    if (refs.forall(_.isInstanceOf[UnresolvedRef[_, _]])) {
      Some(
        refs
          .map(porcelainRefSucc[RefDecl])
          .map(_.get)
      )
    } else {
      None
    }
  }
}