package vct.col.print

import vct.col.ast.{Declaration, Node}
import vct.col.ref.Ref

import scala.util.Try

object Ctx {
  sealed trait Syntax
  case object PVL extends Syntax
  case object Silver extends Syntax
  case object Java extends Syntax
  case object C extends Syntax
  case object CPP extends Syntax
  case object Cuda extends Syntax
  case object OpenCL extends Syntax
}

case class Ctx(
    syntax: Ctx.Syntax = Ctx.PVL,
    width: Int = 120,
    tabWidth: Int = 4,
    names: Map[Declaration[_], String] = Map.empty,
    inSpec: Boolean = false,
) {
  private def usesCIdentifiers: Boolean =
    syntax match {
      case Ctx.C | Ctx.CPP | Ctx.Cuda | Ctx.OpenCL => true
      case _ => false
    }

  private def cIdentifier(name: String): String = {
    val sanitized =
      name.map {
        case c if c.isLetterOrDigit || c == '_' => c
        case _ => '_'
      }.mkString

    if (sanitized.isEmpty || sanitized.head.isDigit)
      "_" + sanitized
    else
      sanitized
  }

  def namesIn[G](node: Node[G]): Ctx =
    copy(names = {
      val namer = Namer[G](syntax)
      namer.name(node)
      namer.finish.asInstanceOf[Map[Declaration[_], String]]
    })

  def name(decl: Declaration[_]): String = {
    val name = names.getOrElse(
      decl,
      s"${decl.o.getPreferredNameOrElse().ucamel}_${decl.hashCode()}",
    )
    // Adjusted for robustness mode: post-resolution C output can synthesize
    // names from type layouts, so keep them valid C identifiers.
    val printableName =
      if (usesCIdentifiers)
        cIdentifier(name)
      else
        name

    if ((inSpec || syntax == Ctx.PVL) && Keywords.SPEC.contains(printableName))
      "`" + printableName + "`"
    else
      printableName
  }

  def name(ref: Ref[_, _ <: Declaration[_]]): String =
    name(Try(ref.decl).getOrElse(return "?brokenref?"))
}
