package org.jetbrains.plugins.scala.lang.psi.api.expr

import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor

/**
* @author Alexander Podkhalyuzin
*/

trait ScThrowStmt extends ScExpression {
  override def accept(visitor: ScalaElementVisitor): Unit = visitor.visitThrowExpression(this)

  def body: Option[ScExpression]
}