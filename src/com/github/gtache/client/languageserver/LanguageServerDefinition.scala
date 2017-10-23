/* Adapted from lsp4e */
package com.github.gtache.client.languageserver

import java.io.{InputStream, OutputStream}

import com.github.gtache.client.LanguageClientImpl
import com.github.gtache.client.connection.{BasicStreamConnectionProvider, ProcessStreamConnectionProvider, StreamConnectionProvider}

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
  def createConnectionProvider(commands: Seq[String] = Seq(), workingDir: String = "", inputStream: InputStream = null, outputStream: OutputStream = null): StreamConnectionProvider = {
    if (streamConnectionProvider == null) {
      if (inputStream != null && outputStream != null) {
        streamConnectionProvider = BasicStreamConnectionProvider(inputStream, outputStream)
      } else {
        streamConnectionProvider = new ProcessStreamConnectionProvider(commands, workingDir)
      }
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
