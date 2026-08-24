package vct.col.ast.declaration.global

import vct.col.ast.{
  ByValueClass,
  Class,
  ClassDeclaration,
  Declaration,
  Expr,
  InstanceField,
  TAnyClass,
  TByReferenceClass,
  TClass,
  TVar,
  Type,
  Variable,
}
import vct.col.ast.util.Declarator
import vct.col.check.{CheckContext, CheckError, SupportNotAClass, TypeError}
import vct.col.origin.Origin
import vct.col.print._
import vct.col.typerules.CoercionUtils
import vct.col.util.AstBuildHelpers.tt

trait ClassImpl[G] extends Declarator[G] {
  this: Class[G] =>
  def typeArgs: Seq[Variable[G]]
  def decls: Seq[ClassDeclaration[G]]
  def supports: Seq[Type[G]]

  def classType(typeArgs: Seq[Type[G]])(implicit o: Origin = this.o): TClass[G]

  def transSupportArrowsHelper(
      seen: Set[TClass[G]]
  ): Seq[(TClass[G], TClass[G])] = {
    val t: TClass[G] = classType(
      typeArgs.map((v: Variable[G]) => TVar(v.ref[Variable[G]]))
    )
    if (seen.contains(t))
      Nil
    else
      supers.map(sup => (t, sup)) ++
        supers.flatMap(sup => sup.transSupportArrowsHelper(Set(t) ++ seen))
  }

  def transSupportArrows: Seq[(TClass[G], TClass[G])] =
    transSupportArrowsHelper(Set.empty)

  def supers: Seq[TClass[G]] = supports.map(_.asClass.get)

  def fields: Seq[InstanceField[G]] =
    decls.collect { case field: InstanceField[G] => field }

  override def declarations: Seq[Declaration[G]] = decls ++ typeArgs

  def layoutLockInvariant(implicit ctx: Ctx): Doc

  def layoutLock(implicit ctx: Ctx): Doc =
    Text("Lock") <+> "intrinsicLock$" <+> "=" <+> "new" <+>
      "ReentrantLock(true);" <+/> "Condition" <+> "condition$" <+> "=" <+>
      "intrinsicLock$" <> "." <> "newCondition()" <> ";"

  def layoutJava(implicit ctx: Ctx): Doc =
    layoutLockInvariant <+/> Group(
      Text("class") <+> ctx.name(this) <>
        (if (typeArgs.nonEmpty)
           Text("<") <> Doc.args(typeArgs) <> ">"
         else
           Empty) <>
        (if (supports.isEmpty)
           // Inheritance still needs work anyway
           Text(" ") <> "extends" <+> "Thread"
         else
           Text(" ") <> "extends" <+> Doc.args(
             supports.map(supp => ctx.name(supp.asClass.get.cls)).map(Text)
           )) <+> "{"
    ) <>> Doc.stack2(layoutLock +: decls) <+/> "}"

  def layoutPvl(implicit ctx: Ctx): Doc =
    layoutLockInvariant <+/> Group(
      Text("class") <+> ctx.name(this) <>
        (if (typeArgs.nonEmpty)
           Text("<") <> Doc.args(typeArgs) <> ">"
         else
           Empty) <>
        (if (supports.isEmpty)
           Empty
         else
           Text(" implements") <+> Doc.args(
             supports.map(supp => ctx.name(supp.asClass.get.cls)).map(Text)
           )) <+> "{"
    ) <>> Doc.stack2(decls) <+/> "}"

  def layoutC(implicit ctx: Ctx): Doc =
    this match {
      case _: ByValueClass[G] =>
        // Adjusted for robustness mode: resolved C structs are represented as
        // by-value classes, but generated mutants must be valid C source.
        Text("struct") <+> ctx.name(this) <+> "{" <>> Doc.stack2(decls) <+/>
          "};"
      case _ => layoutPvl
    }

  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.Java => layoutJava
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP => layoutC
      case _ => layoutPvl
    }

  override def check(context: CheckContext[G]): Seq[CheckError] =
    supports.map(s => (s, CoercionUtils.getCoercion(s, TAnyClass[G]())))
      .collect { case (s, None) => SupportNotAClass(this, s) }
}
