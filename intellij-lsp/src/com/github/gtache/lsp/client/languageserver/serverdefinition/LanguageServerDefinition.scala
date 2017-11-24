package com.github.gtache.lsp.client.languageserver.serverdefinition

import java.io.{InputStream, OutputStream}
import java.net.URI

import com.github.gtache.lsp.client.LanguageClientImpl
import com.github.gtache.lsp.client.connection.StreamConnectionProvider
import com.intellij.openapi.diagnostic.Logger

import scala.collection.mutable

object LanguageServerDefinition {
  val allDefinitions: mutable.Set[LanguageServerDefinition] = mutable.Set()
  private val LOG: Logger = Logger.getInstance(LanguageServerDefinition.getClass)

  /**
    * @return All registered server definitions
    */
  def getAllDefinitions: mutable.Set[LanguageServerDefinition] = allDefinitions.clone()

  /**
    * Register a server definition
    *
    * @param definition The server definition
    */
  def register(definition: LanguageServerDefinition): Unit = {
    if (definition != null) {
      allDefinitions.add(definition)
      LOG.info("Added definition for " + definition)
    } else {
      LOG.warn("Trying to add a null definition")
    }
  }
}

/**
  * A trait representing a ServerDefinition
  */
trait LanguageServerDefinition {
  private val mappedExtensions: mutable.Set[String] = mutable.Set(ext)
  protected var streamConnectionProvider: StreamConnectionProvider = _

  /**
    * @return The extension that the language server manages
    */
  def ext: String

  /**
    * @return The id of the language server (same as extension)
    */
  def id: String = ext


  /**
    * Starts the Language server and returns a tuple (InputStream, OutputStream)
    *
    * @return The input and output streams of the server
    */
  def start(): (InputStream, OutputStream) = {
    if (streamConnectionProvider != null) {
      streamConnectionProvider.start()
      (streamConnectionProvider.getInputStream, streamConnectionProvider.getOutputStream)
    } else (null, null)
  }

  /**
    * Stops the Language server
    */
  def stop(): Unit = {
    if (streamConnectionProvider != null) streamConnectionProvider.stop()
  }

  /**
    * Instantiates a StreamConnectionProvider for this ServerDefinition
    *
    * @param workingDir The current working directory
    * @return The StreamConnectionProvider
    */
  def createConnectionProvider(workingDir: String): StreamConnectionProvider

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

  def getInitializationOptions(uri: URI): Any = null

  override def toString: String = "ServerDefinition for " + ext

}
