package com.github.gtache

import java.io.{InputStream, OutputStream}

import com.intellij.openapi.diagnostic.Logger

import scala.collection.mutable


object ServerDefinitionExtensionPoint {
  val allDefinitions: mutable.Set[ServerDefinitionExtensionPoint] = mutable.Set()

  def getAllDefinitions: mutable.Set[ServerDefinitionExtensionPoint] = allDefinitions.clone()
}

case class ServerDefinitionExtensionPoint(ext: String = "", packge: String = "", mainClass: String = "", args: Array[String] = Array(), inputStream: InputStream = null, outputStream: OutputStream = null) {
  private val LOG: Logger = Logger.getInstance(classOf[ServerDefinitionExtensionPoint])

  import com.github.gtache.ServerDefinitionExtensionPoint._

  allDefinitions.add(this)
  LOG.info("Added definition for " + ext + " : " + packge + " ; " + mainClass + " ; " + args.mkString(" "))
}
