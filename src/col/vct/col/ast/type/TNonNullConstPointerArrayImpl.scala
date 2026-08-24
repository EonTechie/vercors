package vct.col.ast.`type`

import vct.col.ast.{TConstPointerArray, TNonNullConstPointerArray}
import vct.col.ast.ops.TNonNullConstPointerArrayOps
import vct.col.print._

trait TNonNullConstPointerArrayImpl[G] extends TNonNullConstPointerArrayOps[G] {
  this: TNonNullConstPointerArray[G] =>
  private def layoutDimensions(base: Doc)(implicit ctx: Ctx): Doc =
    dimensions.foldLeft(base) {
      case (l, Some(r)) => l <> "[" <> r <> "]"
      case (l, None) => l <> "[]"
    }

  override def layout(implicit ctx: Ctx): Doc =
    Text("NonNull") <+> "const" <+> layoutDimensions(element.show)

  override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Adjusted for robustness mode: non-null is kept in contracts, while
        // the generated source itself must stay valid C.
        val (spec, decl) = element.layoutSplitDeclarator
        (Text("const") <+> spec, decl <> "*")
      case _ => (layout, Empty)
    }

  override val unique: Option[BigInt] = None
  override val isConst: Boolean = true
  override val isNonNull: Boolean = true

  override def descend: TNonNullConstPointerArray[G] =
    TNonNullConstPointerArray(element, dimensions.tail)
  override def asNonNull: TNonNullConstPointerArray[G] = this
  override def asNullable: TConstPointerArray[G] =
    TConstPointerArray(element, dimensions)
}
