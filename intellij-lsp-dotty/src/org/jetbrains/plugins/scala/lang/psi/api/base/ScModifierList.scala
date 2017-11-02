package org.jetbrains.plugins.scala.lang.psi.api.base

import com.intellij.psi.PsiModifierList
import com.intellij.psi.tree.IElementType
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement

/**
  * @author Alexander Podkhalyuzin
  *         Date: 22.02.2008
  */

trait ScModifierList extends ScalaPsiElement with PsiModifierList {
  def has(prop: IElementType): Boolean

  //only one access modifier can occur in a particular modifier list
  def accessModifier: Option[ScAccessModifier]

  def modifiers: Array[String]

  def hasExplicitModifiers: Boolean
}