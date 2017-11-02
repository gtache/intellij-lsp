package org.jetbrains.plugins.scala.lang.psi.impl.expr

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScConstrExpr
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
/**
* @author Alexander Podkhalyuzin
* Date: 22.02.2008
*/

class ScConstrExprImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScConstrExpr {
  override def createMirror(text: String): PsiElement = {
    ScalaPsiElementFactory.createConstructorBodyWithContextFromText(text, getContext, this)
  }

  override def toString: String = "ConstructorExpression"
}