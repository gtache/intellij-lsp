package org.jetbrains.plugins.scala.lang.psi.api.toplevel

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScMember

/**
  * @author Alexander Podkhalyuzin
  *         Date: 22.02.2008
  */
trait ScEarlyDefinitions extends ScalaPsiElement {
  def members: Seq[ScMember]
}