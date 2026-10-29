package vct.col.ast.expr.heap.alloc

import vct.col.ast.{NewConstPointer, TConstPointer, Type}
import vct.col.print._
import vct.col.ast.ops.NewConstPointerOps

trait NewConstPointerImpl[G] extends NewConstPointerOps[G] {
  this: NewConstPointer[G] =>
  override lazy val t: Type[G] = TConstPointer[G](element)

  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Post-resolution C dump: malloc form, not COL `new const T[n]`.
        val (spec, decl) = element.layoutSplitDeclarator
        Text("(") <> "const" <+> spec <> decl <> "*)" <+> "vercorsMalloc" <>
          "(" <> "sizeof" <> "(" <> "const" <+> spec <> decl <> ")" <+> "*" <+>
          size <> ")"
      case _ => Text("new") <+> "const" <+> element <> "[" <> size <> "]"
    }
}
