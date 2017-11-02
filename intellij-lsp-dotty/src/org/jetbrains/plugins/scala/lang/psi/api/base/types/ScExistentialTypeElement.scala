package org.jetbrains.plugins.scala.lang.psi.api.base.types

/**
* @author Alexander Podkhalyuzin
* Date: 13.03.2008
*/

trait ScExistentialTypeElement extends ScTypeElement {
  override protected val typeName = "ExistentialType"

  def quantified: ScTypeElement = findChildByClassScala(classOf[ScTypeElement])
  def clause: ScExistentialClause = findChildByClassScala(classOf[ScExistentialClause])
}