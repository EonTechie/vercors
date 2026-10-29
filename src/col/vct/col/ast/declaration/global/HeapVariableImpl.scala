package vct.col.ast.declaration.global

import vct.col.ast.HeapVariable
import vct.col.print._
import vct.col.ast.ops.HeapVariableOps

trait HeapVariableImpl[G] extends HeapVariableOps[G] {
  this: HeapVariable[G] =>
  override def layout(implicit ctx: Ctx): Doc = {
    val decl: Doc = ctx.syntax match {
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP =>
        // A mutable C global is stored in COL as one non-null pointer layer
        // around its source type. The C dump must remove that storage layer.
        val sourceType = t.asPointer.filter(_.isNonNull).map(_.element).getOrElse(t)
        val (spec, decl) = sourceType.layoutSplitDeclarator
        spec <+> decl <> ctx.name(this)
      case _ => t.show <+> ctx.name(this)

    }
    decl <> init.map(i => Text(" = ") <> i).getOrElse(Text("")) <> ";"
  }
}
