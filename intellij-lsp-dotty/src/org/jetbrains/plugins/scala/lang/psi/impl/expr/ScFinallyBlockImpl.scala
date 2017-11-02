package org.jetbrains.plugins.scala.lang.psi.impl.expr

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.expr._

/** 
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

class ScFinallyBlockImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScFinallyBlock {
  override def toString: String = "FinallyBlock"
}