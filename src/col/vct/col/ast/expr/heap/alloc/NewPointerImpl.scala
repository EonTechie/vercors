package vct.col.ast.expr.heap.alloc

import vct.col.ast.{NewPointer, TPointer, Type}
import vct.col.print._
import vct.col.ast.ops.NewPointerOps

trait NewPointerImpl[G] extends NewPointerOps[G] {
  this: NewPointer[G] =>
  override lazy val t: Type[G] = TPointer[G](element, unique)

  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Post-resolution C dump: malloc form, not COL `new T[n]`.
        val (spec, decl) = element.layoutSplitDeclarator
        Text("(") <> spec <> decl <> "*)" <+> "vercorsMalloc" <> "(" <>
          "sizeof" <> "(" <> spec <> decl <> ")" <+> "*" <+> size <> ")"
      case _ =>
        Text("new") <>
          (if (unique.nonEmpty)
             Text(" unique<" + unique.get.toString + ">")
           else
             Text("")) <+> element <> "[" <> size <> "]"
    }
}
