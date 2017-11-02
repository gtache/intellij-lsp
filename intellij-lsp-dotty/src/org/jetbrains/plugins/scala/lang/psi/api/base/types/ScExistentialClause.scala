package org.jetbrains.plugins.scala.lang.psi.api.base.types

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScDeclaration

/** 
* @author Alexander Podkhalyuzin
* Date: 07.03.2008
*/

trait ScExistentialClause extends ScalaPsiElement {
  def declarations : Seq[ScDeclaration] = findChildrenByClassScala(classOf[ScDeclaration]).toSeq
}