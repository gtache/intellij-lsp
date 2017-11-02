package org.jetbrains.plugins.scala.lang.psi.impl.expr

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.expr._

/** 
* @author Alexander Podkhalyuzin
* Date: 07.03.2008
*/

class ScBindingsImpl(node: ASTNode) extends ScalaPsiElementImpl (node) with ScBindings{
  override def toString: String = "ParameterList"
}