package org.jetbrains.plugins.scala.lang.psi.api.statements

import com.intellij.psi.PsiNamedElement
import org.jetbrains.plugins.scala.extensions.PsiNamedElementExt
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement

trait ScDeclaredElementsHolder extends ScalaPsiElement {
  def declaredElements : Seq[PsiNamedElement]

  def declaredNames: Seq[String] = declaredElements.map(_.name)

  /**
   * @return array for Java compatibility
   */
  def declaredElementsArray : Array[PsiNamedElement] = declaredElements.toArray
}