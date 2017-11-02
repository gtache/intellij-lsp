package org.jetbrains.plugins.scala.lang.psi.impl.base.patterns

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns._
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScBlockExpr

import _root_.scala.collection.mutable._
import scala.collection.mutable

/**
* @author ilyas
*/

class ScPatternArgumentListImpl(node: ASTNode) extends ScalaPsiElementImpl (node) with ScPatternArgumentList{

  override def toString: String = "Pattern Argument List"

  def patterns: mutable.Seq[ScPattern] = {
    val children: Seq[ScPattern] = findChildrenByClassScala[ScPattern](classOf[ScPattern])
    val grandChildrenInBlockExpr: Seq[ScPattern] = this.getChildren.filter{_.isInstanceOf[ScBlockExpr]}.flatMap{s => s.getChildren.filter(_.isInstanceOf[ScPattern]).map{_.asInstanceOf[ScPattern]}}
    children ++ grandChildrenInBlockExpr
  }

}