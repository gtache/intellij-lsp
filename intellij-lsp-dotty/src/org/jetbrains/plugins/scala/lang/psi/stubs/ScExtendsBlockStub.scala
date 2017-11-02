package org.jetbrains.plugins.scala.lang.psi.stubs

import com.intellij.psi.stubs.StubElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScExtendsBlock

/**
  * @author ilyas
  */
trait ScExtendsBlockStub extends StubElement[ScExtendsBlock] {
  def baseClasses: Array[String]
}