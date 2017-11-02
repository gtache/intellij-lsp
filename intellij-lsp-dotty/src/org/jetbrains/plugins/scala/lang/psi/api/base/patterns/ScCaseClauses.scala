package org.jetbrains.plugins.scala.lang.psi.api.base.patterns

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement

/**
* @author Alexander Podkhalyuzin
* Date: 28.02.2008
*/

trait ScCaseClauses extends ScalaPsiElement {
  def caseClause: ScCaseClause = findChildByClassScala(classOf[ScCaseClause])
  def caseClauses: Seq[ScCaseClause] = findChildrenByClassScala(classOf[ScCaseClause]).toSeq
}

object ScCaseClauses {
  def unapplySeq(e: ScCaseClauses): Some[Seq[ScCaseClause]] = Some(e.caseClauses)
}