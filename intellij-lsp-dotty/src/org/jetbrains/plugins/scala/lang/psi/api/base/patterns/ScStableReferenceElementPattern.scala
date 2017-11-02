package org.jetbrains.plugins.scala.lang.psi.api.base.patterns

import org.jetbrains.plugins.scala.lang.psi.api.base.ScReferenceElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression

/**
 * @author ilyas
 */

trait ScStableReferenceElementPattern extends ScPattern {

  def refElement: Option[ScReferenceElement] = findChild(classOf[ScReferenceElement])

  def getReferenceExpression: Option[ScReferenceExpression] = findChild(classOf[ScReferenceExpression])

}