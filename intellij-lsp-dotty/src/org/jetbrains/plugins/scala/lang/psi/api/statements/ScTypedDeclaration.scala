package org.jetbrains.plugins.scala.lang.psi.api.statements

import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable

trait ScTypedDeclaration extends ScDeclaration with Typeable {
  def declaredElements: Seq[ScTypedDefinition]
}