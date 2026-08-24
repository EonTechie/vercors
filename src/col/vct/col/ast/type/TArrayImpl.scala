package vct.col.ast.`type`

import vct.col.ast.TArray
import vct.col.print._
import vct.col.ast.ops.TArrayOps

trait TArrayImpl[G] extends TArrayOps[G] {
  this: TArray[G] =>
  override def layout(implicit ctx: Ctx): Doc = element.show <> "[]"

  override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Adjusted for robustness mode: post-resolution arrays in C function
        // parameters are emitted in the parseable array-to-pointer form.
        val (spec, decl) = element.layoutSplitDeclarator
        (spec, decl <> "*")
      case _ => (layout, Empty)
    }
}
