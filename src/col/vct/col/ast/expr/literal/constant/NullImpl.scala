package vct.col.ast.expr.literal.constant

import vct.col.ast.{Null, TNull, Type}
import vct.col.print.{Ctx, Doc, Precedence, Text}
import vct.col.ast.ops.NullOps

trait NullImpl[G] extends NullOps[G] {
  this: Null[G] =>
  override def t: Type[G] = TNull()

  override def precedence: Int = Precedence.ATOMIC
  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      // Adjusted for robustness mode: generated post-resolution C mutants must
      // use the C null constant when contracts are printed back as C.
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL => Text("NULL")
      case _ => Text("null")
    }
}
