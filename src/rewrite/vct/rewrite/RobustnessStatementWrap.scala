package vct.col.rewrite

import vct.col.ast._
import vct.col.origin.Origin

/** Shared wrap helpers for AddIfZero / AddIfOne. */
object RobustnessStatementWrap {
  def asCompound[G](stat: Statement[G], origin: Origin): Statement[G] =
    stat match {
      case scope @ Scope(Nil, Block(_)) => scope
      case other => Scope[G](Nil, Block[G](Seq(other))(origin))(origin)
    }

  /**
   * Loop/if printers only emit `{ }` for Block/Scope bodies. Wrapping a
   * compound site (LOOP_BODY, then-arm, ...) replaces that Scope with a
   * Branch; without this, C prints `for (...) if (0)` as if the wrap sat
   * in the loop header.
   */
  def keepSurroundingBraces[Pre, Post](
      original: Statement[Pre],
      wrapped: Statement[Post],
  ): Statement[Post] =
    original match {
      case Scope(_, Block(_)) | Block(_) => asCompound(wrapped, wrapped.o)
      case _ => wrapped
    }
}
