package vct.col.ast.node

import vct.col.ast.{Node, Program}
import vct.col.ast.util.Declarator
import vct.col.check.{CheckContext, CheckError}
import vct.col.print.{Ctx, Doc, Empty, Line, Text}
import vct.col.util.CurrentCheckProgramContext
import vct.result.VerificationError
import vct.col.ast.ops.{ProgramFamilyOps, ProgramOps}

trait ProgramImpl[G]
    extends Declarator[G] with ProgramOps[G] with ProgramFamilyOps[G] {
  this: Program[G] =>
  def check: Seq[CheckError] = checkTrans(CheckContext())

  override def checkContextRecursor[T](
      context: CheckContext[G],
      f: (CheckContext[G], Node[G]) => T,
  ): Seq[T] =
    VerificationError.withContext(CurrentCheckProgramContext(this)) {
      super.checkContextRecursor(context, f)
    }

  override def layout(implicit ctx: Ctx): Doc = {
    val decls = declarations

    val header: Doc =
      ctx.syntax match {
        case Ctx.Java =>
          Text("import java.util.concurrent.locks.Lock;") <+/>
            "import java.util.concurrent.locks.ReentrantLock;" <+/>
            "import java.util.concurrent.locks.Condition;" <> Line
        case Ctx.C | Ctx.Cuda | Ctx.OpenCL | Ctx.CPP =>
          // Post-resolution C dump: NewPointer/FreePointer print these names.
          // Emit stubs when the resolved program did not already carry them.
          val names = decls.flatMap { d =>
            try
              Some(ctx.name(d))
            catch { case _: Throwable => None }
          }.toSet
          val parts = Seq(
            Option.when(!names.contains("vercorsMallocNonNull"))(
              Text("void *vercorsMallocNonNull(int size);")
            ),
            Option.when(!names.contains("vercorsMalloc"))(
              Text("void *vercorsMalloc(int size);")
            ),
            Option.when(!names.contains("vercorsFree"))(
              Text("void vercorsFree(void *ptr);")
            ),
          ).flatten
          if (parts.isEmpty)
            Empty
          else
            Doc.stack(parts) <> Line <> Line
        case _ => Empty
      }

    header <> Doc.stack2(decls)
  }
}
