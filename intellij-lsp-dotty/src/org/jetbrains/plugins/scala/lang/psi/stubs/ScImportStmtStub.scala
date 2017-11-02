package org.jetbrains.plugins.scala.lang.psi.stubs

import com.intellij.psi.stubs.StubElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportStmt

/**
  * User: Alexander Podkhalyuzin
  * Date: 18.06.2009
  */
trait ScImportStmtStub extends StubElement[ScImportStmt] {
  def importText: String
}