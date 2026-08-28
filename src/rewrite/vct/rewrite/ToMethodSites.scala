package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
import vct.col.ref.Ref
import vct.col.resolve.ctx.{RefCLocalDeclaration, RefCParam, RefVariable}
import vct.col.resolve.lang.C

/** Places SemTransforms `to_method` may touch: a `{ ... }` that has no
  * `return`/`break`/`continue`/`goto`, no declaration, and no nested loop.
  * An `if` inside is fine. Assignments and `--`/`++` are not sites by
  * themselves (only the surrounding braces).
  */
object ToMethodSites {
  final case class Site[G](
      functionName: String,
      path: String,
      target: Statement[G],
  ) {
    def description: String = describe(target)
  }

  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" || name == "abort" || name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  def isCompound[G](stat: Statement[G]): Boolean =
    stat match {
      case Block(stmts) if stmts.nonEmpty => true
      case Scope(_, Block(stmts)) if stmts.nonEmpty => true
      case Scope(Nil, body) => isCompound(body)
      case _ => false
    }

  private def forbidden[G](node: Node[G]): Boolean =
    node match {
      case _: Return[G] | _: Throw[G] | _: Break[G] | _: Continue[G] |
          _: Goto[G] | _: CGoto[G] =>
        true
      case _: Loop[G] => true
      case _: Switch[G] => true
      case _: LocalDecl[G] | _: HeapLocalDecl[G] |
          _: CDeclarationStatement[G] =>
        true
      case Scope(locals, _) if locals.nonEmpty => true
      case other => other.subnodes.exists(forbidden)
    }

  def selectable[G](stat: Statement[G]): Boolean =
    isCompound(stat) && !forbidden(stat)

  def isCapturedLocal[G](local: CLocal[G]): Boolean =
    local.ref match {
      case Some(_: RefCParam[G]) => true
      case Some(_: RefCLocalDeclaration[G]) => true
      case Some(RefVariable(_)) => true
      case _ => false
    }

  def capturedVars[G](stat: Statement[G]): Seq[Variable[G]] = {
    val result = ArrayBuffer.empty[Variable[G]]
    val seen = ArrayBuffer.empty[Node[G]]

    def go(node: Node[G]): Unit = {
      if (seen.exists(_ eq node))
        return
      seen += node

      node match {
        case Local(Ref(v)) =>
          if (!result.exists(_ eq v))
            result += v
        case _: Type[G] =>
        case _ =>
          node.subnodes.foreach(go)
      }
    }

    go(stat)
    result.toSeq
  }

  private def describe[G](stat: Statement[G]): String =
    stat match {
      case Block(stats) => s"Compound(items=${stats.size})"
      case Scope(locals, Block(stats)) =>
        s"Compound(locals=${locals.size}, items=${stats.size})"
      case Scope(_, body) => s"Compound(body=${body.getClass.getSimpleName})"
      case _ => stat.getClass.getSimpleName
    }

  def collect[G](program: Program[G]): Seq[Site[G]] = {
    val result = ArrayBuffer.empty[Site[G]]

    def add(functionName: String, path: String, stat: Statement[G]): Unit =
      if (selectable(stat)) {
        result += Site(functionName, path, stat)
      }

    def descend(functionName: String, path: String, stat: Statement[G]): Unit =
      stat match {
        case Scope(_, Block(statements)) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            val childPath = s"$path.block[$index]"
            add(functionName, childPath, child)
            descend(functionName, childPath, child)
          }

        case Block(statements) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            val childPath = s"$path.block[$index]"
            add(functionName, childPath, child)
            descend(functionName, childPath, child)
          }

        case Scope(_, loop @ Loop(_, _, _, _, _)) =>
          descend(functionName, s"$path.loop", loop)

        case Branch(branches) =>
          branches.zipWithIndex.foreach { case ((_, body), index) =>
            add(functionName, s"$path.branch[$index]", body)
            descend(functionName, s"$path.branch[$index]", body)
          }

        case Loop(_, _, _, _, body) =>
          val bodyPath = s"$path.body"
          add(functionName, bodyPath, body)
          descend(functionName, bodyPath, body)

        case IndetBranch(branches) =>
          branches.zipWithIndex.foreach { case (body, index) =>
            add(functionName, s"$path.indet[$index]", body)
            descend(functionName, s"$path.indet[$index]", body)
          }

        case Switch(_, body) =>
          descend(functionName, s"$path.switchBody", body)

        case Label(_, inner, _) =>
          descend(functionName, s"$path.labelBody", inner)

        case _ =>
      }

    def procedureName(procedure: Procedure[G]): String =
      procedure.o.getPreferredName.map(_.snake).getOrElse("<anonymous>")

    def visitGlobal(decl: GlobalDeclaration[G]): Unit =
      decl match {
        case unit: CTranslationUnit[G] => unit.declarations.foreach(visitGlobal)

        case function: CFunctionDefinition[G] =>
          val functionName = C.getDeclaratorInfo(function.declarator).name
          if (!blacklistedFunction(functionName)) {
            descend(functionName, s"$functionName.body", function.body)
          }

        case procedure: Procedure[G] =>
          val name = procedureName(procedure)
          if (!blacklistedFunction(name)) {
            procedure.body.foreach(descend(name, s"$name.body", _))
          }

        case _ =>
      }

    program.declarations.foreach(visitGlobal)
    result.toSeq
  }
}
