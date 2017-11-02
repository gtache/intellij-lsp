package org.jetbrains.plugins.scala.lang.psi.impl.expr

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.expr._


/**
* @author ilyas
*/
class ScBlockImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScBlock {
  override def toString: String = "BlockOfExpressions"
}