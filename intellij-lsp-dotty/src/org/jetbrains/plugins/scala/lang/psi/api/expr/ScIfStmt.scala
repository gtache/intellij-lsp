package org.jetbrains.plugins.scala.lang.psi.api.expr

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor


/** 
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

trait ScIfStmt extends ScExpression {
  def condition: Option[ScExpression]
  def thenBranch : Option[ScExpression]
  def elseBranch : Option[ScExpression]
  def getLeftParenthesis : Option[PsiElement]
  def getRightParenthesis : Option[PsiElement]
  override def accept(visitor: ScalaElementVisitor): Unit = visitor.visitIfStatement(this)
}

object ScIfStmt {
  def unapply(ifStmt: ScIfStmt) = Some(ifStmt.condition, ifStmt.thenBranch, ifStmt.elseBranch)
}