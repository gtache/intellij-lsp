package com.github.gtache.client.languageserver.serverdefinition

import com.github.gtache.client.connection.{ProcessStreamConnectionProvider, StreamConnectionProvider}
import com.intellij.openapi.diagnostic.Logger

case class ExeLanguageServerDefinition(ext: String, path: String, args: Array[String]) extends UserConfigurableServerDefinition {
  override def createConnectionProvider(workingDir: String): StreamConnectionProvider = {
    if (streamConnectionProvider == null) {
      streamConnectionProvider = new ProcessStreamConnectionProvider(Seq(path) ++ args, workingDir)
    }
    streamConnectionProvider
  }

  override def toString: String = super.toString + " exe : " + path + " args : " + args.mkString(" ")

  override def toArray: Array[String] = {
    Array(typ, ext, path) ++ args
  }

}

object ExeLanguageServerDefinition extends UserConfigurableServerDefinitionObject {
  private val LOG: Logger = Logger.getInstance(this.getClass)

  override def fromArray(arr: Array[String]): ExeLanguageServerDefinition = {
    if (arr.head == typ) {
      val arrTail = arr.tail
      if (arrTail.length < 2) {
        LOG.warn("Not enough elements to translate into a ServerDefinition : " + arr)
        null
      } else {
        ExeLanguageServerDefinition(arrTail.head, arrTail.tail.head, if (arrTail.length > 2) arrTail.tail.tail else Array())
      }
    } else {
      null
    }
  }

  override def typ: String = "exe"
}