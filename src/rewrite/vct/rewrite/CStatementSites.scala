package vct.col.rewrite

import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
import vct.col.ref.Ref
import vct.col.resolve.lang.C

object CStatementSites {
  sealed trait Role {
    def label: String
  }

  final case class BlockItem(index: Int) extends Role {
    override def label: String = s"BLOCK_ITEM[$index]"
  }

  final case class BranchArm(index: Int) extends Role {
    override def label: String =
      if (index == 0)
        "IF_TRUE"
      else
        s"IF_ELSE[$index]"
  }

  case object LoopBody extends Role {
    override def label: String = "LOOP_BODY"
  }

  final case class Site[G](
      functionName: String,
      path: String,
      role: Role,
      target: Statement[G],
  ) {
    def description: String = describe(target)
  }

  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" || name == "abort" || name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  private def abruptControl[G](stat: Statement[G]): Boolean =
    stat match {
      case _: Return[G] | _: Throw[G] | _: Break[G] | _: Continue[G] |
          _: Goto[G] | _: CGoto[G] =>
        true
      case _ => false
    }

  def containsAbruptControl[G](stat: Statement[G]): Boolean =
    stat match {
      // Robustness mode: never create an if(1) wrapper around return-like
      // control flow, even when it is hidden inside a source compound node.
      case _ if abruptControl(stat) => true
      case Scope(_, body) => containsAbruptControl(body)
      case Block(statements) => statements.exists(containsAbruptControl)
      case Branch(branches) =>
        branches.exists { case (_, body) => containsAbruptControl(body) }
      case IndetBranch(branches) => branches.exists(containsAbruptControl)
      case Loop(_, _, _, _, body) => containsAbruptControl(body)
      case Switch(_, body) => containsAbruptControl(body)
      case Label(_, inner, _) => containsAbruptControl(inner)
      case _ => false
    }

  private def declarationCluster[G](stat: Statement[G]): Boolean =
    stat match {
      case _: CDeclarationStatement[G] | _: LocalDecl[G] | _: HeapLocalDecl[G] =>
        true

      case Scope(Nil, body) => declarationCluster(body)

      case Block(Seq(inner)) => declarationCluster(inner)

      case Block(Seq(LocalDecl(local), AssignInitial(Local(Ref(v)), _))) =>
        v eq local

      case Block(Seq(LocalDecl(local), Assign(Local(Ref(v)), _))) =>
        v eq local

      case _ => false
    }

  private def selectableShape[G](stat: Statement[G]): Boolean =
    stat match {
      case _ if containsAbruptControl(stat) => false
      case _ if declarationCluster(stat) => false
      case _: NonExecutableStatement[G] => false

      case _: AssignStmt[G] => true
      case _: Eval[G] => true
      case _: InvocationStatement[G] => true
      case _: Branch[G] => true
      case Block(_) => true
      case Scope(_, Block(_)) => true
      case Scope(_, Loop(_, _, _, _, _)) => true
      case _: Loop[G] => true

      case _ => false
    }

  private def selectable[G](role: Role, stat: Statement[G]): Boolean =
    selectableShape(stat)

  private def describe[G](stat: Statement[G]): String =
    stat match {
      case Scope(locals, Block(stats)) =>
        s"CompoundStatement(locals=${locals.size}, items=${stats.size})"
      case Scope(_, Loop(init, cond, update, _, body)) =>
        s"LoopScope(init=${init.getClass.getSimpleName}, " +
          s"cond=${cond.getClass.getSimpleName}, " +
          s"update=${update.getClass.getSimpleName}, " +
          s"body=${body.getClass.getSimpleName})"
      case Loop(init, cond, update, _, body) =>
        s"LoopStatement(init=${init.getClass.getSimpleName}, " +
          s"cond=${cond.getClass.getSimpleName}, " +
          s"update=${update.getClass.getSimpleName}, " +
          s"body=${body.getClass.getSimpleName})"
      case Branch(branches) => s"IfStatement(arms=${branches.size})"
      case _: AssignStmt[G] => "AssignmentStatement"
      case Eval(expr) =>
        s"ExpressionStatement(expr=${expr.getClass.getSimpleName})"
      case _: InvocationStatement[G] => "InvocationStatement"
      case _: CGoto[G] | _: Goto[G] => "GotoStatement"
      case _: Break[G] => "BreakStatement"
      case _: Continue[G] => "ContinueStatement"
      case _: Return[G] => "ReturnStatement"
      case _: CDeclarationStatement[G] => "DeclarationStatement"
      case other => other.getClass.getSimpleName
    }

  def collect[G](program: Program[G]): Seq[Site[G]] = {
    val result = ArrayBuffer.empty[Site[G]]

    def add(
        functionName: String,
        path: String,
        role: Role,
        target: Statement[G],
    ): Unit =
      if (selectable(role, target)) {
        result += Site(functionName, path, role, target)
      }

    def descend(functionName: String, path: String, stat: Statement[G]): Unit =
      stat match {
        case _ if declarationCluster(stat) =>

        case Scope(_, Block(statements)) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            val childPath = s"$path.block[$index]"
            add(functionName, childPath, BlockItem(index), child)
            descend(functionName, childPath, child)
          }

        case Block(statements) =>
          statements.zipWithIndex.foreach { case (child, index) =>
            val childPath = s"$path.block[$index]"
            add(functionName, childPath, BlockItem(index), child)
            descend(functionName, childPath, child)
          }

        case Scope(_, loop @ Loop(_, _, _, _, _)) =>
          descend(functionName, s"$path.loop", loop)

        case Branch(branches) =>
          branches.zipWithIndex.foreach { case ((_, body), index) =>
            add(functionName, s"$path.branch[$index]", BranchArm(index), body)
          }

          branches.zipWithIndex.foreach { case ((_, body), index) =>
            descend(functionName, s"$path.branch[$index]", body)
          }

        case Loop(_, _, _, _, body) =>
          val bodyPath = s"$path.body"
          add(functionName, bodyPath, LoopBody, body)
          descend(functionName, bodyPath, body)

        case IndetBranch(branches) =>
          branches.zipWithIndex.foreach { case (body, index) =>
            add(functionName, s"$path.indet[$index]", BranchArm(index), body)
          }

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
