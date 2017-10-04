package com.github.gtache.client

import scala.collection.mutable

class LanguageServerDefinition(val id: String) {

  private val mappedExtensions: mutable.Set[String] = mutable.Set()
  private var streamConnectionProvider: StreamConnectionProvider = _

  def addMappedExtension(ext: String): Unit = {
    mappedExtensions.add(ext)
  }

  def removeMappedExtension(ext: String): Unit = {
    mappedExtensions.remove(ext)
  }

  def createConnectionProvider(commands: Seq[String], workingDir: String): StreamConnectionProvider = {
    if (streamConnectionProvider == null) {
      streamConnectionProvider = new ProcessStreamConnectionProvider(commands, workingDir)
    }
    streamConnectionProvider
  }

  def getMappedExtensions: mutable.Set[String] = mappedExtensions.clone()

  def createLanguageClient: LanguageClientImpl = new LanguageClientImpl

}
