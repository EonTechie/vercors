package vct.col.ast.`type`

import vct.col.ast.TBool
import vct.col.print.{Ctx, Doc, Text}
import vct.col.ast.ops.TBoolOps
import vct.col.typerules.TypeSize

trait TBoolImpl[G] extends TBoolOps[G] {
  this: TBool[G] =>
  override def bits: TypeSize = TypeSize.Exact(BigInt.int2bigInt(8))
  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.PVL => Text("boolean")
      case Ctx.Silver => Text("Bool")
      case Ctx.Java => Text("boolean")
      // Post-resolution C pretty-print: ISO C uses `_Bool`; `bool` is not
      // a C99 keyword. Cuda / OpenCL / CPP keep `bool`.
      case Ctx.C => Text("_Bool")
      case Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => Text("bool")
    }
}
