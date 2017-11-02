 package org.jetbrains.plugins.scala.lang.psi.api.expr

/**
* @author Alexander.Podkhalyuzin 
*/

trait ScConstrBlock extends ScBlockExpr {
  def selfInvocation: Option[ScSelfInvocation] = findChild(classOf[ScSelfInvocation])
}