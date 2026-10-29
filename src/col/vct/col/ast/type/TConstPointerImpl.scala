package vct.col.ast.`type`

import vct.col.ast.{TConstPointer, TConst, Type}
import vct.col.ast.ops.TConstPointerOps
import vct.col.print._

trait TConstPointerImpl[G] extends TConstPointerOps[G] {
  this: TConstPointer[G] =>
  val unique: Option[BigInt] = None

  val isConst = true
  val isNonNull = false

  override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) = {
    val (spec, decl) = element.layoutSplitDeclarator
    (Text("const") <+> spec, decl <> "*")
  }

  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Post-resolution C dump: print as const T*, not COL const_pointer<T>.
        val (spec, decl) = layoutSplitDeclarator
        Group(spec <> decl)
      case _ => Text("const_pointer") <> open <> element <> close
    }

  override def asNullable: Type[G] = this
}
