package vct.col.ast.lang.smt

import vct.col.ast.{SmtlibIsInt, TBool, Type}
import vct.col.print._
import vct.col.ast.ops.SmtlibIsIntOps

trait SmtlibIsIntImpl[G] extends SmtlibIsIntOps[G] {
  this: SmtlibIsInt[G] =>
  override def t: Type[G] = TBool()
  override def layout(implicit ctx: Ctx): Doc =
    // Spec grammar uses \is_int; bare is_int does not resolve on re-parse.
    Text("\\is_int(") <> arg <> ")"
}
