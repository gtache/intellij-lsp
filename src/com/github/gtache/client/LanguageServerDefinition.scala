/* Adapted from lsp4e */
package com.github.gtache.client

import scala.collection.mutable

/**
  * Class representing the definition of a language server (related extensions)
  *
  * @param id the id of the language
  */
class LanguageServerDefinition(val id: String) {

  private val mappedExtensions: mutable.Set[String] = mutable.Set()
  private var streamConnectionProvider: StreamConnectionProvider = _

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
    * Instantiates a ProcessStreamConnectionProvider for this LanguageServer
    *
    * @param commands   The commands to run
    * @param workingDir The current working directory
    * @return The StreamConnectionProvider
    */
  def createConnectionProvider(commands: Seq[String], workingDir: String): StreamConnectionProvider = {
    if (streamConnectionProvider == null) {
      streamConnectionProvider = new ProcessStreamConnectionProvider(commands, workingDir)
    }
    streamConnectionProvider
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
