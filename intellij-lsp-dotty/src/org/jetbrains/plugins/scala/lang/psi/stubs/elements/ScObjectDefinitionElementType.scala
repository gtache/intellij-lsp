package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.ScObjectImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTemplateDefinitionStub

/**
 * @author ilyas
 */
class ScObjectDefinitionElementType extends ScTemplateDefinitionElementType[ScObject]("object definition") {
  override def createElement(node: ASTNode): ScObject = new ScObjectImpl(node)

  override def createPsi(stub: ScTemplateDefinitionStub): ScObject = new ScObjectImpl(stub)
}
