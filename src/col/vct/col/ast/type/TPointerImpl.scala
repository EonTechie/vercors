package vct.col.ast.`type`

import vct.col.ast.{TPointer, Type}
import vct.col.print._
import vct.col.ast.ops.TPointerOps

trait TPointerImpl[G] extends TPointerOps[G] {
  this: TPointer[G] =>

  val isConst = false
  val isNonNull = false

  override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) = {
    val (spec, decl) = element.layoutSplitDeclarator
    (spec, decl <> "*")
  }

  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        // Post-resolution C dump: print as C pointer syntax, not COL pointer<T>.
        val (spec, decl) = layoutSplitDeclarator
        Group(spec <> decl)
      case _ =>
        Group(
          Text(
            (if (unique.isDefined)
               "unique<" + unique.get.toString + ">"
             else
               "") + "pointer"
          ) <> open <> element <> close
        )
    }

  override def asNullable: Type[G] = this
}
