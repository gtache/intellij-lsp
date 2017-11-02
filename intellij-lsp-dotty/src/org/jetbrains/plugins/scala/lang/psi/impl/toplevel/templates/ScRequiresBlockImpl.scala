package org.jetbrains.plugins.scala.lang.psi.impl.toplevel.templates

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates._

/** 
* @author Alexander Podkhalyuzin
* Date: 20.02.2008
*/

class ScRequiresBlockImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScRequiresBlock{
  override def toString: String = "RequiresBlock"
}