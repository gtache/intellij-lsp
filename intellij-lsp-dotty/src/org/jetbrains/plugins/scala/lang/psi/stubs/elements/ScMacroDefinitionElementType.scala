package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScMacroDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.statements.ScMacroDefinitionImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.ScFunctionStub

/**
  * User: Jason Zaugg
  */
class ScMacroDefinitionElementType extends ScFunctionElementType("macro definition") {
  override def createElement(node: ASTNode): ScMacroDefinition = new ScMacroDefinitionImpl(node)

  override def createPsi(stub: ScFunctionStub): ScMacroDefinition = new ScMacroDefinitionImpl(stub)
}