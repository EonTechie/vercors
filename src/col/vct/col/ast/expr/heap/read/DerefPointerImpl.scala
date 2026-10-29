package vct.col.ast.expr.heap.read

import vct.col.ast.{DerefHeapVariable, DerefPointer, Type}
import vct.col.print._
import vct.col.ast.ops.DerefPointerOps

trait DerefPointerImpl[G] extends DerefPointerOps[G] {
  this: DerefPointer[G] =>
  override def t: Type[G] = pointer.t.asPointer.get.element

  override def precedence: Int = Precedence.PREFIX
  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP =>
        pointer match {
          // LangCToCol represents mutable globals through an artificial
          // non-null storage pointer. The source C access has no `*` for that
          // representation layer.
          case DerefHeapVariable(ref) if ref.decl.t.asPointer.exists(_.isNonNull) =>
            pointer.show
          case _ => Text("*") <> assoc(pointer)
        }
      case _ => Text("*") <> assoc(pointer)
    }
}
