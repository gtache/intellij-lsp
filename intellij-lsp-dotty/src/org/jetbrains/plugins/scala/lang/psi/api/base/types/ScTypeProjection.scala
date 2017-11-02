package org.jetbrains.plugins.scala.lang.psi.api.base.types

import org.jetbrains.plugins.scala.lang.psi.api.base.ScReferenceElement

/**
* @author Alexander Podkhalyuzin
* Date: 13.03.2008
*/

trait ScTypeProjection extends ScTypeElement with ScReferenceElement {
  override protected val typeName = "TypeProjection"

  def typeElement: ScTypeElement = findChildByClassScala(classOf[ScTypeElement])
}