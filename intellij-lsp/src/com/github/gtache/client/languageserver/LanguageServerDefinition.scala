package com.github.gtache.client.languageserver

import java.io.{InputStream, OutputStream}

import com.github.gtache.client.LanguageClientImpl
import com.github.gtache.client.connection.StreamConnectionProvider
import com.intellij.openapi.diagnostic.Logger

import scala.collection.mutable

object LanguageServerDefinition {
  val allDefinitions: mutable.Set[LanguageServerDefinition] = mutable.Set()
  private val LOG: Logger = Logger.getInstance(LanguageServerDefinition.getClass)

  def getAllDefinitions: mutable.Set[LanguageServerDefinition] = allDefinitions.clone()

  def register(definition: LanguageServerDefinition): Unit = {
    allDefinitions.add(definition)
    LOG.info("Added definition for " + definition)
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
  def start(): (InputStream, OutputStream)

  /**
    * Stops the Language server
    */
  def stop(): Unit

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

  override def toString: String = "ServerDefinition for " + ext

}
