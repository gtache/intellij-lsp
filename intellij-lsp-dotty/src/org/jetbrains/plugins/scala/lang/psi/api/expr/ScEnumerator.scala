package org.jetbrains.plugins.scala.lang.psi.api.expr

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor

/** 
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

trait ScEnumerator extends ScalaPsiElement with ScPatterned {
  def rvalue: ScExpression

  def valKeyword: Option[PsiElement] = {
    Option(getNode.findChildByType(ScalaTokenTypes.kVAL)).map(_.getPsi)
  }

  override def accept(visitor: ScalaElementVisitor): Unit = visitor.visitEnumerator(this)
}