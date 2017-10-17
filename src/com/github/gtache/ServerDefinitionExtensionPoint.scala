package com.github.gtache

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName

import scala.collection.mutable

object ServerDefinitionExtensionPoint {
  val EP_NAME: ExtensionPointName[ServerDefinitionExtensionPoint] = ExtensionPointName.create[ServerDefinitionExtensionPoint]("com.github.gtache.ServerDefinitionExtensionPoint")
  val allDefinitions: mutable.Set[ServerDefinitionExtensionPoint] = mutable.Set()

  def getAllDefinitions: mutable.Set[ServerDefinitionExtensionPoint] = allDefinitions.clone()
}

case class ServerDefinitionExtensionPoint(ext: String, packge: String, mainClass: String, args: Array[String]) {
  private val LOG: Logger = Logger.getInstance(classOf[ServerDefinitionExtensionPoint])

  import ServerDefinitionExtensionPoint._

  allDefinitions.add(this)
  LOG.info("Added definition for " + ext + " : " + packge + " ; " + mainClass + " ; " + args.mkString(" "))
}
