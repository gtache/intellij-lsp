package org.jetbrains.plugins.scala.lang.psi.api.expr

/**
* @author Alexander Podkhalyuzin
* Date: 07.03.2008
*/

trait ScParenthesisedExpr extends ScInfixArgumentExpression {
  def expr: Option[ScExpression] = findChild(classOf[ScExpression])
}

object ScParenthesisedExpr {
  def unapply(p: ScParenthesisedExpr): Option[ScExpression] = p.expr
}