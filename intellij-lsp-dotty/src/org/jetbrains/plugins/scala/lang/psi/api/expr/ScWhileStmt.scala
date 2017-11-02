package org.jetbrains.plugins.scala.lang.psi.api.expr

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor


/** 
* @author Alexander Podkhalyuzin
*/

trait ScWhileStmt extends ScExpression {
  def condition: Option[ScExpression]

  def body: Option[ScExpression]

  def getLeftParenthesis : Option[PsiElement]

  def getRightParenthesis : Option[PsiElement]

  override def accept(visitor: ScalaElementVisitor): Unit = visitor.visitWhileStatement(this)
}

object ScWhileStmt {
  def unapply(statement: ScWhileStmt): Option[(Option[ScExpression], Option[ScExpression])] =
    Some((statement.condition, statement.body))
}