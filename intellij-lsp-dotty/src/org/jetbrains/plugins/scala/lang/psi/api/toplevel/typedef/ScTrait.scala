package org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef

import com.intellij.psi.{PsiClass, PsiElement}
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.light.PsiClassWrapper

/** 
* @author Alexander Podkhalyuzin
* @since 20.02.2008
*/

trait ScTrait extends ScTypeDefinition {
  def getTraitToken: PsiElement = findFirstChildByType(ScalaTokenTypes.kTRAIT)
  def getObjectClassOrTraitToken: PsiElement = getTraitToken

  def fakeCompanionClass: PsiClass = {
    def withSuffix(name: String) = s"$name$$class"
    new PsiClassWrapper(this, withSuffix(getQualifiedName), withSuffix(getName))
  }
}