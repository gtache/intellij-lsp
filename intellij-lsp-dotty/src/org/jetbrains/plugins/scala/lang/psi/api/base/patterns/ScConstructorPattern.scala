package org.jetbrains.plugins.scala.lang.psi.api.base.patterns

import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReferenceElement

/**
* @author Alexander Podkhalyuzin
* Patterns, introduced by case classes or extractors
*/
trait ScConstructorPattern extends ScPattern {
  def args: ScPatternArgumentList = findChildByClassScala(classOf[ScPatternArgumentList])
  def ref: ScStableCodeReferenceElement = findChildByClassScala(classOf[ScStableCodeReferenceElement])
}