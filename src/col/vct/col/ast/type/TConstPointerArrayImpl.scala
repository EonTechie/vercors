package vct.col.ast.`type`

import vct.col.ast.{TConstPointerArray, TNonNullConstPointerArray}
import vct.col.ast.ops.TConstPointerArrayOps
import vct.col.print._

trait TConstPointerArrayImpl[G] extends TConstPointerArrayOps[G] {
  this: TConstPointerArray[G] =>
  private def layoutDimensions(base: Doc)(implicit ctx: Ctx): Doc =
    dimensions.foldLeft(base) {
      case (l, Some(r)) => l <> "[" <> r <> "]"
      case (l, None) => l <> "[]"
    }

  override def layout(implicit ctx: Ctx): Doc =
    Text("const") <+> layoutDimensions(element.show)

  override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Adjusted for robustness mode: keep const pointer arrays in C
        // declarator form when post-resolution mutants are printed.
        val (spec, decl) = element.layoutSplitDeclarator
        (Text("const") <+> spec, decl <> "*")
      case _ => (layout, Empty)
    }

  override val unique: Option[BigInt] = None
  override val isConst: Boolean = true
  override val isNonNull: Boolean = false

  override def descend: TConstPointerArray[G] =
    TConstPointerArray(element, dimensions.tail)
  override def asNonNull: TNonNullConstPointerArray[G] =
    TNonNullConstPointerArray(element, dimensions)
  override def asNullable: TConstPointerArray[G] = this
}
