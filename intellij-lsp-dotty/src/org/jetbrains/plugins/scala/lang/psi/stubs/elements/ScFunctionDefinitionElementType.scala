package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.statements.ScFunctionDefinitionImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.ScFunctionStub

/**
  * User: Alexander Podkhalyuzin
  * Date: 14.10.2008
  */
class ScFunctionDefinitionElementType extends ScFunctionElementType("function definition") {
  override def createElement(node: ASTNode): ScFunctionDefinition = new ScFunctionDefinitionImpl(node)

  override def createPsi(stub: ScFunctionStub): ScFunctionDefinition = new ScFunctionDefinitionImpl(stub)
}