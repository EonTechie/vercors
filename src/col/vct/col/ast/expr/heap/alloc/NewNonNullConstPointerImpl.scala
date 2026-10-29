package vct.col.ast.expr.heap.alloc

import vct.col.ast.{NewNonNullConstPointer, Type, TNonNullConstPointer}
import vct.col.print._
import vct.col.ast.ops.NewNonNullConstPointerOps

trait NewNonNullConstPointerImpl[G] extends NewNonNullConstPointerOps[G] {
  this: NewNonNullConstPointer[G] =>
  override lazy val t: Type[G] = TNonNullConstPointer[G](element)

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
