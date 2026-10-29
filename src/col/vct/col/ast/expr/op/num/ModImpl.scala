package vct.col.ast.expr.op.num

import vct.col.ast.{Mod, TInt, Type}
import vct.col.print.{Ctx, Doc, Group, Precedence, Text}
import vct.col.ast.ops.ModOps

trait ModImpl[G] extends ModOps[G] {
  this: Mod[G] =>
  override def t: Type[G] = TInt()
  override def precedence: Int = Precedence.MULTIPLICATIVE
  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL =>
        Group(Text("\\euclidean_mod(") <> Doc.args(Seq(left, right)) <> ")")
      case _ => lassoc(left, "%", right)
    }
}
