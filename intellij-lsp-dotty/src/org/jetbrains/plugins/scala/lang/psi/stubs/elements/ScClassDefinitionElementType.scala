package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.ScClassImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTemplateDefinitionStub

/**
 * @author ilyas
 */
class ScClassDefinitionElementType extends ScTemplateDefinitionElementType[ScClass]("class definition") {
  override def createElement(node: ASTNode): ScClass = new ScClassImpl(node)

  override def createPsi(stub: ScTemplateDefinitionStub): ScClass = new ScClassImpl(stub)
}
