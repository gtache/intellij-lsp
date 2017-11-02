package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariableDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.statements.ScVariableDefinitionImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.ScVariableStub

/**
  * User: Alexander Podkhalyuzin
  * Date: 18.10.2008
  */
class ScVariableDefinitionElementType extends ScVariableElementType[ScVariableDefinition]("variable definition") {
  override def createElement(node: ASTNode): ScVariableDefinition = new ScVariableDefinitionImpl(node)

  override def createPsi(stub: ScVariableStub): ScVariableDefinition = new ScVariableDefinitionImpl(stub)
}