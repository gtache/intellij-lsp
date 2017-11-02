package org.jetbrains.plugins.scala.lang.psi.stubs.index

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariable

/**
  * User: Alexander Podkhalyuzin
  * Date: 18.10.2008
  */
class ScVariableNameIndex extends ScStringStubIndexExtension[ScVariable] {

  override def getKey: StubIndexKey[String, ScVariable] =
    ScalaIndexKeys.VARIABLE_NAME_KEY
}
