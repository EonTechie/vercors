package vct.col.rewrite

import vct.col.ast._
import RewriteHelpers._
import vct.col.origin.Origin
import vct.col.util.AstBuildHelpers._
import vct.col.rewrite.{Generation, Rewriter, RewriterBuilder}

case object ForLoopToWhileLoop extends RewriterBuilder {
  override def key: String = "forLoop"
  override def desc: String =
    "Translate for loops into while loops by putting initialization and the update before and in the loop."

  def rewriteLoop[Pre <: Generation, Post <: Generation](
      loop: Loop[Pre],
      rewriteDefaultLoop: Loop[Pre] => Statement[Post],
      dispatchStatement: Statement[Pre] => Statement[Post],
      dispatchExpr: Expr[Pre] => Expr[Post],
      dispatchContract: LoopContract[Pre] => LoopContract[Post],
  ): Statement[Post] =
    loop match {
      case Loop(Block(Nil), _, Block(Nil), _, _) =>
        rewriteDefaultLoop(loop)

      case Loop(init, cond, update, contract, body) =>
        implicit val o: Origin = loop.o
        Block[Post](Seq(
          dispatchStatement(init),
          Loop[Post](
            Block[Post](Nil),
            dispatchExpr(cond),
            Block[Post](Nil),
            dispatchContract(contract),
            Block[Post](Seq(
              dispatchStatement(body),
              dispatchStatement(update),
            )),
          ),
        ))
    }
}

case class ForLoopToWhileLoop[Pre <: Generation]() extends Rewriter[Pre] {
  override def dispatch(stat: Statement[Pre]): Statement[Post] =
    stat match {
      case loop: Loop[Pre] =>
        ForLoopToWhileLoop.rewriteLoop(
          loop,
          (loop: Loop[Pre]) => loop.rewriteDefault(),
          (stat: Statement[Pre]) => dispatch(stat),
          (expr: Expr[Pre]) => dispatch(expr),
          (contract: LoopContract[Pre]) => dispatch(contract),
        )

      case other => other.rewriteDefault()
    }
}
