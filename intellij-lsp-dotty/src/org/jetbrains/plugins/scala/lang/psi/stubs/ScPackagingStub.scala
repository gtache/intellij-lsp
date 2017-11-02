package org.jetbrains.plugins.scala.lang.psi.stubs

import com.intellij.psi.stubs.StubElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging

/**
  * @author ilyas
  */
trait ScPackagingStub extends StubElement[ScPackaging] {
  def parentPackageName: String

  def packageName: String

  def isExplicit: Boolean
}