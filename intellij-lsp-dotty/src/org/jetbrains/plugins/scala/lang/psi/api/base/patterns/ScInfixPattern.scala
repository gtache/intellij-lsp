package org.jetbrains.plugins.scala.lang.psi.api.base.patterns

import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReferenceElement

/**
* @author Alexander Podkhalyuzin
* Date: 28.02.2008
*/

trait ScInfixPattern extends ScPattern {
  def leftPattern: ScPattern = findChildByClassScala(classOf[ScPattern])
  def rightPattern: Option[ScPattern] = findLastChild(classOf[ScPattern])
  def reference: ScStableCodeReferenceElement = findChildByClassScala(classOf[ScStableCodeReferenceElement])
}