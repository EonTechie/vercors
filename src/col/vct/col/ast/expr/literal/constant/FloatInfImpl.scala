package vct.col.ast.expr.literal.constant

import vct.col.ast.FloatInf
import vct.col.ast.ops.FloatInfOps
import vct.col.print._

trait FloatInfImpl[G] extends FloatInfOps[G] {
  this: FloatInf[G] =>
  val value: Double = Double.PositiveInfinity
  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      // VerCors C lexer/parser keyword (see LangCLexer INFINITY).
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => Text("vercorsINFINITY")
      case _ => Text("inf")
    }
}
