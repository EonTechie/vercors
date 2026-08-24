package vct.col.ast.`type`

import vct.col.ast.{TNonNullPointerArray, TPointerArray}
import vct.col.ast.ops.TPointerArrayOps
import vct.col.print._

trait TPointerArrayImpl[G] extends TPointerArrayOps[G] {
  this: TPointerArray[G] =>
  private def layoutDimensions(base: Doc)(implicit ctx: Ctx): Doc =
    dimensions.foldLeft(base) {
      case (l, Some(r)) => l <> "[" <> r <> "]"
      case (l, None) => l <> "[]"
    }

  override def layout(implicit ctx: Ctx): Doc =
    layoutDimensions(
      unique.map(u => Text(s"unique<$u>") <+> element).getOrElse(element.show)
    )

  override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Adjusted for robustness mode: emit parseable C pointer syntax instead
        // of generic COL array syntax in generated mutants.
        val (spec, decl) = element.layoutSplitDeclarator
        (spec, decl <> "*")
      case _ => (layout, Empty)
    }

  override val isConst: Boolean = false
  override val isNonNull: Boolean = false

  override def descend: TPointerArray[G] =
    TPointerArray(element, dimensions.tail, unique)
  override def asNonNull: TNonNullPointerArray[G] =
    TNonNullPointerArray(element, dimensions, unique)
  override def asNullable: TPointerArray[G] = this
}
