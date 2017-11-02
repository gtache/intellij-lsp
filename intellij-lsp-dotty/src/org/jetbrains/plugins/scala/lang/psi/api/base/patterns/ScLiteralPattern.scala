package org.jetbrains.plugins.scala.lang.psi.api.base.patterns

import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral

/**
* @author Alexander Podkhalyuzin
* Date: 28.02.2008
*/

trait ScLiteralPattern extends ScPattern {
  def getLiteral: ScLiteral = findChildByClassScala(classOf[ScLiteral])
}