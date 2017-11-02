package org.jetbrains.plugins.scala.lang.psi.api.base.patterns

/**
* @author Alexander Podkhalyuzin
*/

trait ScParenthesisedPattern extends ScPattern {
  def subpattern: Option[ScPattern] = findChild(classOf[ScPattern])
}

object ScParenthesisedPattern {
  def unapply(e: ScParenthesisedPattern): Option[ScPattern] = e.subpattern
}