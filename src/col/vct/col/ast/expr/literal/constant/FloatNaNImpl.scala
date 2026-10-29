package vct.col.ast.expr.literal.constant

import vct.col.ast.FloatNaN
import vct.col.ast.ops.FloatNaNOps
import vct.col.print._

trait FloatNaNImpl[G] extends FloatNaNOps[G] {
  this: FloatNaN[G] =>
  def value: Double = Double.NaN
  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      // VerCors C lexer/parser keyword (see LangCLexer NAN).
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => Text("vercorsNAN")
      case _ => Text("NaN")
    }
}
