package vct.col.ast.`type`

import vct.col.ast.node.NodeImpl
import vct.col.ast.{
  ByValueClass,
  Class,
  InstanceField,
  TByReferenceClass,
  TByValueClass,
  TClass,
  TClassUnique,
  Type,
  Variable,
}
import vct.col.check.{CheckContext, CheckError, TypeErrorExplanation}
import vct.col.print._
import vct.col.ref.Ref

trait TClassImpl[G] extends NodeImpl[G] {
  this: TClass[G] =>
  def cls: Ref[G, Class[G]]

  def typeArgs: Seq[Type[G]]

  def transSupportArrowsHelper(
      seen: Set[TClass[G]]
  ): Seq[(TClass[G], TClass[G])] =
    cls.decl.transSupportArrowsHelper(seen).map { case (clsA, clsB) =>
      (instantiate(clsA).asClass.get, instantiate(clsB).asClass.get)
    }

  def transSupportArrows(): Seq[(TClass[G], TClass[G])] =
    transSupportArrowsHelper(Set.empty)

  override def check(context: CheckContext[G]): Seq[CheckError] =
    if (cls.decl.typeArgs.length == typeArgs.length) { Nil }
    else
      Seq(TypeErrorExplanation(
        this,
        s"type has ${typeArgs.length} type arguments, but class definition has ${cls
            .decl.typeArgs.length} type arguments",
      ))

  private def layoutCName(implicit ctx: Ctx): Doc =
    cls.decl match {
      case _: ByValueClass[G] =>
        // Adjusted for robustness mode: by-value classes originate from C
        // structs, so references to them must be emitted as `struct T`.
        Text("struct") <+> ctx.name(cls)
      case _ => Text(ctx.name(cls))
    }

  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => Group(layoutCName)
      case _ =>
        Group(
          Text(ctx.name(cls)) <>
            (if (typeArgs.nonEmpty)
               Text("<") <> Doc.args(typeArgs) <> ">"
             else
               Empty)
        )
    }

  override def layoutSplitDeclarator(implicit ctx: Ctx): (Doc, Doc) =
    ctx.syntax match {
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => (layoutCName, Empty)
      case _ => (layout, Empty)
    }

  def typeEnv: Map[Variable[G], Type[G]] = cls.decl.typeArgs.zip(typeArgs).toMap

  def instantiate(t: Type[G]): Type[G] =
    this match {
      case TByReferenceClass(Ref(cls), typeArgs) if typeArgs.nonEmpty =>
        t.particularize(cls.typeArgs.zip(typeArgs).toMap)
      case TByValueClass(Ref(cls), typeArgs) if typeArgs.nonEmpty =>
        t.particularize(cls.typeArgs.zip(typeArgs).toMap)
      case t: TClassUnique[G] if t.typeArgs.nonEmpty => ??? // TODO
      case _ => t
    }

  def fieldType(decl: InstanceField[G]): Type[G] = instantiate(decl.t)
}
