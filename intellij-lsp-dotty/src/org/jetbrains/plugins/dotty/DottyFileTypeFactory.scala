package org.jetbrains.plugins.dotty

import com.intellij.openapi.fileTypes.FileTypeConsumer
import org.jetbrains.plugins.scala.ScalaFileTypeFactory


class DottyFileTypeFactory extends ScalaFileTypeFactory {
  override def createFileTypes(consumer: FileTypeConsumer): Unit = {
    consumer.consume(DottyFileType.INSTANCE, "scala")
  }
}