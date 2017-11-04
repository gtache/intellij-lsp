package com.github.gtache.client.languageserver.serverdefinition

import com.github.gtache.client.connection.{ProcessStreamConnectionProvider, StreamConnectionProvider}
import com.github.gtache.utils.CoursierImpl
import com.intellij.openapi.diagnostic.Logger

/**
  * Represents a ServerDefinition for a LanguageServer stored on a repository
  *
  * @param ext       The extension that the server manages
  * @param packge    The artifact id of the server
  * @param mainClass The main class of the server
  * @param args      The arguments to give to the main class
  */
case class ArtifactLanguageServerDefinition(ext: String, packge: String, mainClass: String, args: Array[String]) extends UserConfigurableServerDefinition {

  override def createConnectionProvider(workingDir: String): StreamConnectionProvider = {
    if (streamConnectionProvider == null) {
      val cp = CoursierImpl.resolveClasspath(packge)
      streamConnectionProvider = new ProcessStreamConnectionProvider(Seq("java", "-cp", cp, mainClass) ++ args, workingDir)
    }
    streamConnectionProvider
  }

  override def toString: String = super.toString + " artifact : " + packge + " mainClass : " + mainClass + " args : " + args.mkString(" ")

  override def toArray: Array[String] = {
    Array(typ, ext, packge, mainClass) ++ args
  }

}

object ArtifactLanguageServerDefinition extends UserConfigurableServerDefinitionObject {
  private val LOG: Logger = Logger.getInstance(this.getClass)

  def fromArray(arr: Array[String]): ArtifactLanguageServerDefinition = {
    if (arr.head == typ) {
      val arrTail = arr.tail
      if (arrTail.length < 3) {
        LOG.warn("Not enough elements to translate into a ServerDefinition : " + arr)
        null
      } else {
        ArtifactLanguageServerDefinition(arrTail.head, arrTail.tail.head, arrTail.tail.tail.head, if (arrTail.length > 3) arrTail.tail.tail.tail else Array())
      }
    } else {
      null
    }
  }

  def typ: String = "artifact"
}
