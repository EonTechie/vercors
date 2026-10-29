package vct.col.ast.expr.literal.build

import vct.col.ast.{LiteralVector, TVector, Type}
import vct.col.print.{Ctx, Doc, Group, Precedence, Text}
import vct.col.ast.ops.LiteralVectorOps
import vct.col.ast.`type`.TVectorImpl

trait LiteralVectorImpl[G] extends LiteralVectorOps[G] {
  this: LiteralVector[G] =>
  override def t: Type[G] = TVector(values.size, element)()

  override def precedence: Int = Precedence.POSTFIX
  override def layout(implicit ctx: Ctx): Doc =
    ctx.syntax match {
      case Ctx.C | Ctx.Cuda | Ctx.OpenCL if !ctx.inSpec =>
        // OpenCL-style vector literal: (T)(e0, e1, ...). Brace compound
        // literals do not parse for this type form in LangCParser.
        // Cast args to the element type so float vectors accept int literals.
        val args = values.map(v => Text("(") <> element <> Text(")") <> v)
        Text("(") <> TVectorImpl.cTypeDoc(element, values.size) <> Text(")(") <>
          Doc.args(args) <> Text(")")
      case _ if ctx.inSpec || ctx.syntax == Ctx.PVL =>
        // SpecParser: vector<T>{...} (no size in the literal).
        Group(Text("vector<") <> element <> ">{" <> Doc.args(values) <> "}")
      case _ =>
        Group(
          Text("vector<") <> element <> "," <> values.size.toString <> ">{" <>
            Doc.args(values) <> "}"
        )
    }
}
