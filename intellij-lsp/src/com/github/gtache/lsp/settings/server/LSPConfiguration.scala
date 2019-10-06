package com.github.gtache.lsp.settings.server

import java.io.File

import com.github.gtache.lsp.settings.server.parser.ConfigurationParser

import scala.collection.JavaConverters._

case class LSPConfiguration(settings: Map[String, Map[String, AnyRef]]) {
  def getJavaSettings: java.util.Map[String, java.util.Map[String, AnyRef]] = {
    settings.map(pair => pair._1 -> pair._2.asJava).asJava
  }

  def getSettings: Map[String, Map[String, AnyRef]] = settings
}

object LSPConfiguration {

  def forFile(file: File): LSPConfiguration = {
    ConfigurationParser.getConfiguration(file)
  }

  //TODO manage User config < Workspace config
  def forFiles(files: Seq[File]): LSPConfiguration = {
    val configs = files.map(forFile).map(_.settings)
    LSPConfiguration(configs.foldLeft(Map[String, Map[String, AnyRef]]())((configTop, configBottom) => {
      ConfigurationParser.combineConfigurations(configTop, configBottom)
    }))
  }

  val emptyConfiguration: LSPConfiguration = LSPConfiguration(Map("global" -> Map[String, AnyRef]()))

}