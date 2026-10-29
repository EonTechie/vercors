package vct.col.ast.`type`

import vct.col.ast.TUnique
import vct.col.ast.ops.TUniqueOps
import vct.col.print._
import vct.col.typerules.TypeSize

trait TUniqueImpl[G] extends TUniqueOps[G] {
  this: TUnique[G] =>
  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        Text("/*@ unique<") <> unique.toString <> "> @*/" <+> inner
      case _ => Text("unique<") <> unique.toString <> ">" <+> inner
    }

  override def bits: TypeSize = inner.bits
}
