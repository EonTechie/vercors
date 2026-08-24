package vct.col.ast.`type`

import vct.col.ast.{TNonNullPointerArray, TPointerArray}
import vct.col.ast.ops.TNonNullPointerArrayOps
import vct.col.print._

trait TNonNullPointerArrayImpl[G] extends TNonNullPointerArrayOps[G] {
  this: TNonNullPointerArray[G] =>
  private def layoutDimensions(base: Doc)(implicit ctx: Ctx): Doc =
    dimensions.foldLeft(base) {
      case (l, Some(r)) => l <> "[" <> r <> "]"
      case (l, None) => l <> "[]"
    }

  override def layout(implicit ctx: Ctx): Doc =
    layoutDimensions(
      Text("NonNull") <+> unique.map(u => Text(s"unique<$u>") <+> element)
        .getOrElse(element.show)
    )

  override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Adjusted for robustness mode: non-null is a verifier property, not
        // C declarator syntax, so keep the emitted mutant parseable as C.
        val (spec, decl) = element.layoutSplitDeclarator
        (spec, decl <> "*")
      case _ => (layout, Empty)
    }

  override val isConst: Boolean = false
  override val isNonNull: Boolean = true

  override def descend: TNonNullPointerArray[G] =
    TNonNullPointerArray(element, dimensions.tail, unique)
  override def asNonNull: TNonNullPointerArray[G] = this
  override def asNullable: TPointerArray[G] =
    TPointerArray(element, dimensions, unique)
}
