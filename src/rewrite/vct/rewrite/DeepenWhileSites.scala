package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
import vct.col.origin.LabelContext
import vct.col.resolve.lang.C

/** Places deepen-while may touch: a `while` whose contract is
  * `LoopInvariant`, with no assignment or call in the condition, and no
  * `break` / `continue` / `goto` for this loop. A `for` is not a site.
  * `IterationContract` is not a site.
  *
  * Only the inner adapter while created by deepen-while is excluded here.
  * The selected outer while stays a candidate, so `--robustness-repeat`
  * may nest another adapter under the same `while (C)`. Other transforms
  * do not read this marker.
  */
object DeepenWhileSites {
  val GeneratedLabel: String = "deepen-while-generated"
  final case class Site[G](
      functionName: String,
      path: String,
      target: Loop[G],
  ) {
    def description: String =
      s"While(generated=${isGenerated(target)}, cond=${target.cond.getClass.getSimpleName}, body=${describe(target.body)})"
  }

  def isWhile[G](loop: Loop[G]): Boolean =
    loop match {
      case Loop(Block(Nil), _, Block(Nil), _, _) => true
      case _ => false
    }

  def isGenerated[G](loop: Loop[G]): Boolean =
    loop.o.originContents.exists {
      case LabelContext(GeneratedLabel) => true
      case _ => false
    }

  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" || name == "abort" || name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  private def containsForbiddenControl[G](
      stat: Statement[G],
      inSwitch: Boolean = false,
  ): Boolean =
    stat match {
      case _: Break[G] => !inSwitch
      case _: Continue[G] | _: Goto[G] | _: CGoto[G] => true
      case _: Loop[G] => false
      case Block(statements) =>
        statements.exists(containsForbiddenControl(_, inSwitch))
      case Scope(_, body) => containsForbiddenControl(body, inSwitch)
      case Branch(branches) =>
        branches.exists { case (_, body) =>
          containsForbiddenControl(body, inSwitch)
        }
      case IndetBranch(branches) =>
        branches.exists(containsForbiddenControl(_, inSwitch))
      case Switch(_, body) => containsForbiddenControl(body, inSwitch = true)
      case Label(_, inner, _) => containsForbiddenControl(inner, inSwitch)
      case _ => false
    }

  private def condHasAssignOrCall[G](expr: Expr[G]): Boolean = {
    def go(node: Node[G]): Boolean =
      node match {
        case _: AssignExpression[G] => true
        case _: InvokingNode[G] => true
        case other => other.subnodes.exists(go)
      }
    go(expr)
  }

  def selectable[G](loop: Loop[G]): Boolean =
    isWhile(loop) && !isGenerated(loop) && (loop.contract match {
      case _: LoopInvariant[G] => true
      case _: IterationContract[G] => false
    }) && !containsForbiddenControl(loop.body) &&
      !condHasAssignOrCall(loop.cond)

  private def describe[G](stat: Statement[G]): String =
    stat match {
      case Block(stats) => s"Block(items=${stats.size})"
      case Scope(locals, Block(stats)) =>
        s"Scope(locals=${locals.size}, items=${stats.size})"
      case Scope(_, body) => s"Scope(body=${body.getClass.getSimpleName})"
      case _ => stat.getClass.getSimpleName
    }

  def collect[G](program: Program[G]): Seq[Site[G]] = {
    val result = ArrayBuffer.empty[Site[G]]

    def add(functionName: String, path: String, loop: Loop[G]): Unit = {
      if (isWhile(loop) && isGenerated(loop)) {
        println(
          s"[DeepenWhile] skip generated adapter" +
            s" | function=$functionName" +
            s" | path=$path"
        )
      }
      if (selectable(loop)) {
        result += Site(functionName, path, loop)
      }
    }

    def descend(functionName: String, path: String, stat: Statement[G]): Unit =
      stat match {
        case Block(statements) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            descend(functionName, s"$path.block[$index]", child)
          }

        case Scope(_, body) =>
          descend(functionName, s"$path.scope", body)

        case loop @ Loop(_, _, _, _, body) =>
          add(functionName, path, loop)
          descend(functionName, s"$path.body", body)

        case Branch(branches) =>
          branches.zipWithIndex.foreach { case ((_, body), index) =>
            descend(functionName, s"$path.branch[$index]", body)
          }

        case IndetBranch(branches) =>
          branches.zipWithIndex.foreach { case (body, index) =>
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
