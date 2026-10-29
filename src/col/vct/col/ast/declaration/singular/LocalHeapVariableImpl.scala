package vct.col.ast.declaration.singular

import vct.col.ast.LocalHeapVariable
import vct.col.print._
import vct.col.ast.ops.{LocalHeapVariableOps, VariableFamilyOps}
import vct.col.ast.ops.{LocalHeapVariableOps, LocalHeapVariableFamilyOps}

trait LocalHeapVariableImpl[G]
    extends LocalHeapVariableOps[G] with LocalHeapVariableFamilyOps[G] {
  this: LocalHeapVariable[G] =>
  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP =>
        // Post-resolution C dump: @heap is COL-only; emit a normal C local.
        val (spec, decl) = t.layoutSplitDeclarator
        val declaration = spec <+> decl <> ctx.name(this)
        t.asPointer.filter(_.isNonNull) match {
          case Some(pointer) =>
            val (elementSpec, elementDecl) = pointer.element
              .layoutSplitDeclarator
            declaration <+> "=" <+>
              (Text("(") <> elementSpec <> elementDecl <> "*)" <+>
                "vercorsMallocNonNull" <> "(" <> "sizeof" <> "(" <>
                elementSpec <> elementDecl <> ")" <> ")")
          case None => declaration
        }
      case Ctx.PVL | Ctx.Java =>
        Text("@heap") <+> t.show <+> ctx.name(this)
      case Ctx.Silver =>
        Text("@heap") <+> Text(ctx.name(this)) <> ":" <+> t
    }
}
