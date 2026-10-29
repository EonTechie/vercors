package vct.col.ast.`type`

import vct.col.ast.{TVector, Type}
import vct.col.ast.ops.TVectorOps
import vct.col.print.{Ctx, Doc, Empty, Group, Text}

object TVectorImpl {
  /** C dump form accepted by LangCParser as a typeSpecifier (not a typedef
    * name). Typedef-based compound literals conflict with castExpression's
    * `isTypedefName` gate, so we avoid typedefs here.
    */
  def cTypeDoc[G](element: Type[G], size: BigInt)(implicit ctx: Ctx): Doc =
    Text("__opencl_vector_type__(") <> element <> Text(", ") <>
      Text(size.toString) <> Text(")")
}

trait TVectorImpl[G] extends TVectorOps[G] {
  this: TVector[G] =>

  override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) =
    ctx.syntax match {
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL if !ctx.inSpec =>
        (TVectorImpl.cTypeDoc(element, size), Empty)
      case _ =>
        (show, Empty)
    }

  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL if !ctx.inSpec =>
        TVectorImpl.cTypeDoc(element, size)
      case _ =>
        Group(
          Text("vector") <> open <> Doc.arg(element) <> "," <> size.toString <>
            close
        )
    }

  val subtypes: Seq[Type[G]] = Seq(element)
}
