package com.github.gtache

import java.io.{InputStream, OutputStream}

import com.github.gtache.client.LanguageClientImpl
import com.github.gtache.client.connection.{ProcessStreamConnectionProvider, StreamConnectionProvider}
import com.intellij.openapi.diagnostic.Logger

import scala.collection.mutable


object ServerDefinitionExtensionPoint {
  val allDefinitions: mutable.Set[ServerDefinitionExtensionPoint] = mutable.Set()

  def getAllDefinitions: mutable.Set[ServerDefinitionExtensionPoint] = allDefinitions.clone()
}

case class ServerDefinitionExtensionPoint(ext: String, packge: String = "", mainClass: String = "", args: Array[String] = Array()) {
  val id : String = ext
  private val LOG: Logger = Logger.getInstance(this.getClass)
  private val mappedExtensions: mutable.Set[String] = mutable.Set(ext)
  private var streamConnectionProvider: StreamConnectionProvider = _

  import com.github.gtache.ServerDefinitionExtensionPoint._

  allDefinitions.add(this)
  LOG.info("Added definition for " + ext + " : " + packge + " ; " + mainClass + " ; " + args.mkString(" "))

  def start(): (InputStream, OutputStream) = {
    streamConnectionProvider.start()
    (streamConnectionProvider.getInputStream, streamConnectionProvider.getOutputStream)
  }


  def stop(): Unit = {
    streamConnectionProvider.stop()
  }

  /**
    * Instantiates a StreamConnectionProvider for this ServerDefinition
    * Warning: Long running, run asynchronously
    *
    * @param workingDir The current working directory
    * @return The StreamConnectionProvider
    */
  def createConnectionProvider(workingDir: String): StreamConnectionProvider = {
    if (streamConnectionProvider == null) {
      if (isArtifactDependent) {
        val cp = CoursierImpl.resolveClasspath(packge)
        streamConnectionProvider = new ProcessStreamConnectionProvider(Seq("java", "-cp", cp, mainClass) ++ args, workingDir)
      } else {

      }
    }
    streamConnectionProvider
  }

  def isArtifactDependent: Boolean = true

  /**
    * Adds a file extension for this LanguageServer
    *
    * @param ext the extension
    */
  def addMappedExtension(ext: String): Unit = {
    mappedExtensions.add(ext)
  }

  /**
    * Removes a file extension for this LanguageServer
    *
    * @param ext the extension
    */
  def removeMappedExtension(ext: String): Unit = {
    mappedExtensions.remove(ext)
  }


  /**
    * @return the extensions linked to this LanguageServer
    */
  def getMappedExtensions: mutable.Set[String] = mappedExtensions.clone()

  /**
    * @return the LanguageClient for this LanguageServer
    */
  def createLanguageClient: LanguageClientImpl = new LanguageClientImpl
}
