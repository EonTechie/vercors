package vct.col.ast.expr.heap.alloc

import vct.col.ast.ops.NewPointerArrayOps
import vct.col.ast.{NewPointerArray, TNonNullPointerArray, Type}
import vct.col.print._

trait NewPointerArrayImpl[G] extends NewPointerArrayOps[G] {
  this: NewPointerArray[G] =>
  override def t: Type[G] =
    TNonNullPointerArray(element, dimensions.map(Some(_)), unique)
  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Post-resolution C dump: one-dimensional malloc; multi-dim stays best-effort.
        val (spec, decl) = element.layoutSplitDeclarator
        val sizeDoc: Doc =
          if (dimensions.isEmpty)
            Text("1")
          else
            dimensions.map(_.show).reduceLeft[Doc]((a, b) => a <+> "*" <+> b)
        Text("(") <> spec <> decl <> "*)" <+> "vercorsMallocNonNull" <> "(" <>
          "sizeof" <> "(" <> spec <> decl <> ")" <+> "*" <+> sizeDoc <> ")"
      case _ =>
        Text("new") <+> unique.map(u => Text(s"unique<$u>") <+> element)
          .getOrElse(element.show) <> dimensions.mkString("[", ",", "]")
    }
}
