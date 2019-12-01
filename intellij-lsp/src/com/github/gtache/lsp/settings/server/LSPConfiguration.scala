package com.github.gtache.lsp.settings.server

import java.io.File

import com.github.gtache.lsp.settings.server.parser.ConfigurationParser

import scala.collection.JavaConverters._

case class LSPConfiguration(settings: Map[String, Map[String, AnyRef]]) {
  def getJavaSettings: java.util.Map[String, java.util.Map[String, AnyRef]] = {
    settings.map(pair => pair._1 -> pair._2.asJava).asJava
  }

  def getSettings: Map[String, Map[String, AnyRef]] = settings

  def isValid: Boolean = this != LSPConfiguration.invalidConfiguration

  def getScopeForUri(uri: String): String = "global" //TODO manage different scopes

  def getAttributesForSectionAndScope(section: String, scope: String = "global"): Map[String, AnyRef] = {
    settings.get(scope).map(m => m.filterKeys(key => key.startsWith(section)).map(keyValue => {
      val strippedKey = keyValue._1.stripPrefix(section)
      if (strippedKey.startsWith(".")) {
        strippedKey.stripPrefix(".") -> keyValue._2
      } else {
        strippedKey -> keyValue._2
      }
    })).orNull
  }

  def getAttributesForSectionAndUri(section: String, uri: String): Map[String, AnyRef] = {
    getAttributesForSectionAndScope(section, getScopeForUri(uri))
  }
}

object LSPConfiguration {
  def fromFile(file: File): LSPConfiguration = {
    ConfigurationParser.getConfiguration(file)
  }

  //TODO manage User config < Workspace config
  def fromFiles(files: Seq[File]): LSPConfiguration = {
    val configs = files.map(fromFile).map(_.settings)
    LSPConfiguration(configs.foldLeft(Map[String, Map[String, AnyRef]]())((configTop, configBottom) => {
      ConfigurationParser.combineConfigurations(configTop, configBottom)
    }))
  }

  val emptyConfiguration: LSPConfiguration = LSPConfiguration(Map("global" -> Map[String, AnyRef]()))
  val invalidConfiguration: LSPConfiguration = LSPConfiguration(null)

}