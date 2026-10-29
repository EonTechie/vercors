package vct.col.ast.`type`

import vct.col.ast.{TNonNullPointer, TPointer, Type}
import vct.col.ast.ops.TNonNullPointerOps
import vct.col.print._

trait TNonNullPointerImpl[G] extends TNonNullPointerOps[G] {
  this: TNonNullPointer[G] =>

  val isConst = false
  val isNonNull = true

  override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) = {
    val (spec, decl) = element.layoutSplitDeclarator
    (spec, decl <> "*")
  }

  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Post-resolution C dump: non-null is a verifier property, not C syntax.
        val (spec, decl) = layoutSplitDeclarator
        Group(spec <> decl)
      case _ =>
        Group(
          Text(
            (if (unique.isDefined)
               "unique<" + unique.get.toString + ">"
             else
               "") + "NonNull"
          ) <> open <> element <> close
        )
    }

  override def asNullable: Type[G] = TPointer(element, unique)
}
