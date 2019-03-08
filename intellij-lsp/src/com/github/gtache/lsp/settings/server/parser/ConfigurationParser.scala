package com.github.gtache.lsp.settings.server.parser

import java.io.{BufferedReader, File, FileReader}

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.gtache.lsp.settings.server.LSPConfiguration
import javax.xml.parsers.DocumentBuilderFactory

object ConfigurationParser {

  def getConfiguration(file: File): LSPConfiguration = {
    val parser = forFile(file)
    parser.map(p => {
      val fileContent = new StringBuilder
      val reader = new BufferedReader(new FileReader(file))
      while (reader.ready) {
        fileContent.append(reader.readLine())
      }
      p.parse(fileContent.toString)
    }).orNull
  }

  def getConfiguration(doc: String, typ: ConfigType): LSPConfiguration = {
    val parser = forType(typ)
    parser.map(p => p.parse(doc)).orNull
  }

  def forType(typ: ConfigType): Option[ConfigurationParser] = {
    typ match {
      case ConfigType.JSON => None
      case ConfigType.XML => None
    }
  }

  def forFile(file: File): Option[ConfigurationParser] = {
    val name = file.getName
    val idx = name.lastIndexOf('.')
    if (idx + 1 < name.length) {
      forExt(name.drop(idx + 1))
    } else None
  }

  def forExt(typ: String): Option[ConfigurationParser] = {
    ???
  }
}

trait ConfigurationParser {
  def parse(doc: String): LSPConfiguration
}

class JsonParser extends ConfigurationParser {
  override def parse(doc: String): LSPConfiguration = {
    val map = new ObjectMapper().readValue(doc, classOf[java.util.HashMap[String, AnyRef]])
    ???
  }
}

class XmlParser extends ConfigurationParser {
  override def parse(doc: String): LSPConfiguration = {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    ???
  }
}
