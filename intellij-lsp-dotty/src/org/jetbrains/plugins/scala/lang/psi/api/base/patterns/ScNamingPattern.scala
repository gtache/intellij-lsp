package org.jetbrains.plugins.scala.lang.psi.api.base.patterns

/**
* @author Alexander Podkhalyuzin
*/

trait ScNamingPattern extends ScBindingPattern {
  def named: ScPattern = findChildByClassScala(classOf[ScPattern])
}