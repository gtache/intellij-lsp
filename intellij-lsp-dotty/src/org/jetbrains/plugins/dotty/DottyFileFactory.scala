package org.jetbrains.plugins.dotty

import com.intellij.psi.FileViewProvider
import org.jetbrains.plugins.scala.lang.parser.ScalaFileFactory
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

class DottyFileFactory extends ScalaFileFactory {
  override def createFile(provider: FileViewProvider): Option[ScalaFile] = Option(provider.getVirtualFile.getFileType).collect{
    case f: DottyFileType => new DottyFileImpl(provider)
  }
}
