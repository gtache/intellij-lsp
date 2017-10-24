package com.github.gtache.client.connection

import java.io.{InputStream, OutputStream}

import com.github.gtache.ServerDefinitionExtensionPoint

/**
  * A stream connection provider for ExtensionPoint
  */
abstract class ExtensionStreamConnectionProvider(s: ServerDefinitionExtensionPoint) extends StreamConnectionProvider {
  var inputStream: InputStream = _
  var outputStream: OutputStream = _

  override def start(): Unit = {

  }

  override def getInputStream: InputStream = inputStream

  override def getOutputStream: OutputStream = outputStream

  override def stop(): Unit = {

  }
}
