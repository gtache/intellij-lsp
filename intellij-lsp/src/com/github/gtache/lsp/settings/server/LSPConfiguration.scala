package com.github.gtache.lsp.settings.server

import scala.collection.JavaConverters._

case class LSPConfiguration(settings: Map[String, Map[String, AnyRef]]) {
  def getJavaSettings: java.util.Map[String, java.util.Map[String, AnyRef]] = {
    settings.map(pair => pair._1 -> pair._2.asJava).asJava
  }

  def getSettings: Map[String, Map[String, AnyRef]] = settings
}