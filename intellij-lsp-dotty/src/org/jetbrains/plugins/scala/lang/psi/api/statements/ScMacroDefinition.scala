package org.jetbrains.plugins.scala.lang.psi.api.statements

import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScArguments

/**
 * @author Jason Zaugg
 */
trait ScMacroDefinition extends ScFunction {
  def typeElement = returnTypeElement
  def body: Option[ScExpression]
}