package org.jetbrains.plugins.scala.lang.psi.impl.expr

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.expr._

/** 
* @author Alexander Podkhalyuzin
* Date: 07.03.2008
*/
class ScAnnotationExprImpl(node: ASTNode) extends ScalaPsiElementImpl (node) with ScAnnotationExpr{
  override def toString: String = "AnnotationExpression"
}