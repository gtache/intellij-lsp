package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariableDeclaration
import org.jetbrains.plugins.scala.lang.psi.impl.statements.ScVariableDeclarationImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.ScVariableStub

/**
  * User: Alexander Podkhalyuzin
  * Date: 18.10.2008
  */
class ScVariableDeclarationElementType extends ScVariableElementType[ScVariableDeclaration]("variable declaration") {
  override def createElement(node: ASTNode): ScVariableDeclaration = new ScVariableDeclarationImpl(node)

  override def createPsi(stub: ScVariableStub): ScVariableDeclaration = new ScVariableDeclarationImpl(stub)
}