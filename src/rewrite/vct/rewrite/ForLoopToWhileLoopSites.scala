package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
import vct.col.resolve.lang.C

object ForLoopToWhileLoopSites {
  final case class Site[G](
      functionName: String,
      path: String,
      target: Loop[G],
  ) {
    def description: String =
      s"ForLoop(init=${describe(target.init)}, cond=${target.cond.getClass.getSimpleName}, " +
        s"update=${describe(target.update)}, body=${describe(target.body)})"
  }

  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" || name == "abort" || name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  private def isForLoop[G](loop: Loop[G]): Boolean =
    loop match {
      case Loop(Block(Nil), _, Block(Nil), _, _) => false
      case _ => true
    }

  private def containsContinueForThisLoop[G](stat: Statement[G]): Boolean =
    stat match {
      case _: Continue[G] => true
      case _: Loop[G] => false
      case Block(statements) => statements.exists(containsContinueForThisLoop)
      case Scope(_, body) => containsContinueForThisLoop(body)
      case Branch(branches) =>
        branches.exists { case (_, body) => containsContinueForThisLoop(body) }
      case IndetBranch(branches) =>
        branches.exists(containsContinueForThisLoop)
      case Switch(_, body) => containsContinueForThisLoop(body)
      case Label(_, inner, _) => containsContinueForThisLoop(inner)
      case _ => false
    }

  private def selectable[G](loop: Loop[G]): Boolean =
    isForLoop(loop) && !containsContinueForThisLoop(loop.body)

  private def describe[G](stat: Statement[G]): String =
    stat match {
      case Block(stats) => s"Block(items=${stats.size})"
      case Scope(locals, Block(stats)) =>
        s"Scope(locals=${locals.size}, items=${stats.size})"
      case Scope(_, body) => s"Scope(body=${body.getClass.getSimpleName})"
      case Loop(init, _, update, _, body) =>
        s"Loop(init=${init.getClass.getSimpleName}, " +
          s"update=${update.getClass.getSimpleName}, " +
          s"body=${body.getClass.getSimpleName})"
      case Branch(branches) => s"IfStatement(arms=${branches.size})"
      case _: AssignInitial[G] => "InitialAssignmentStatement"
      case _: AssignStmt[G] => "AssignmentStatement"
      case Eval(expr) =>
        s"ExpressionStatement(expr=${expr.getClass.getSimpleName})"
      case _: InvocationStatement[G] => "InvocationStatement"
      case _: Continue[G] => "ContinueStatement"
      case _: Return[G] => "ReturnStatement"
      case _: CDeclarationStatement[G] | _: LocalDecl[G] => "DeclarationStatement"
      case _: NonExecutableStatement[G] => "NonExecutableStatement"
      case other => other.getClass.getSimpleName
    }

  def collect[G](program: Program[G]): Seq[Site[G]] = {
    val result = ArrayBuffer.empty[Site[G]]

    def add(functionName: String, path: String, loop: Loop[G]): Unit =
      if (selectable(loop)) {
        result += Site(functionName, path, loop)
      }

    def descend(functionName: String, path: String, stat: Statement[G]): Unit =
      stat match {
        case block @ Block(statements) =>
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
