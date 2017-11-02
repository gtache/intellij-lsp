package org.jetbrains.plugins.scala.lang.psi.api.expr

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor

/**
 * @author Alexander Podkhalyuzin
 */

trait ScGuard extends ScalaPsiElement {
  def expr: Option[ScExpression]

  override def accept(visitor: ScalaElementVisitor): Unit = visitor.visitGuard(this)
}