package com.github.gtache.lsp.settings.server.parser

import java.io.{File, FileReader}

import com.github.gtache.lsp.settings.server.LSPConfiguration
import com.google.gson
import com.google.gson._
import com.intellij.openapi.util.io.FileUtilRt
import javax.xml.parsers.DocumentBuilderFactory

import scala.collection.JavaConverters._
import scala.collection.mutable

object ConfigurationParser {

  def getConfiguration(file: File): LSPConfiguration = {
    val parser = forFile(file)
    parser.map(p => {
      p.parse(file)
    }).orNull
  }

  def getConfiguration(doc: String, typ: ConfigType): LSPConfiguration = {
    val parser = forType(typ)
    val file = FileUtilRt.createTempFile("config", "." + ConfigType.toExt(typ), true)
    parser.map(p => p.parse(file)).orNull
  }

  def forType(typ: ConfigType): Option[ConfigurationParser] = {
    typ match {
      case ConfigType.FLAT => Some(new FlatParser())
      case ConfigType.JSON => Some(new JsonParser())
      case ConfigType.XML => Some(new XmlParser())
      case _ => None
    }
  }

  def forFile(file: File): Option[ConfigurationParser] = {
    val name = file.getName
    val idx = name.lastIndexOf('.')
    if (idx + 1 < name.length) {
      forExt(name.drop(idx + 1))
    } else None
  }

  def forExt(ext: String): Option[ConfigurationParser] = {
    forType(ConfigType.forExt(ext))
  }

  def combineConfigurations(firstConfig: Map[String, Map[String, AnyRef]], secondConfig: Map[String, Map[String, AnyRef]]): Map[String, Map[String, AnyRef]] = {
    val concatMap = mutable.Map[String, mutable.Map[String, AnyRef]]()
    firstConfig.keySet.foreach(key => {
      concatMap.update(key, mutable.Map(firstConfig(key).toSeq: _*))
    })
    secondConfig.keySet.foreach(key => {
      if (concatMap.contains(key)) {
        secondConfig(key).keySet.foreach(subkey => concatMap(key).update(subkey, secondConfig(key)(subkey)))
      } else {
        concatMap.update(key, mutable.Map(secondConfig(key).toSeq: _*))
      }
    })
    concatMap.map(pair => pair._1 -> pair._2.toMap).toMap
  }
}

trait ConfigurationParser {
  def parse(file: File): LSPConfiguration
}

class JsonParser extends ConfigurationParser {
  override def parse(file: File): LSPConfiguration = {
    val reader = new FileReader(file)
    val jsonObject = new gson.JsonParser().parse(reader).getAsJsonObject
    val configMap = Map[String, Map[String, AnyRef]]("global" -> Map[String, AnyRef]())

    def flatten(scope: String, key: String, elem: JsonElement, map: Map[String, Map[String, AnyRef]]): Map[String, Map[String, AnyRef]] = {
      var trueScope = scope
      var trueKey = key
      if (scope == null) {
        if (trueKey == null) {
          trueScope = null
          trueKey = null
        }
        else if (trueKey != null && trueKey.startsWith("[") && trueKey.endsWith("]")) {
          if (!elem.isJsonObject) {
            throw new IllegalArgumentException("Check JSON file " + file.getAbsolutePath)
          }
          trueScope = key
          trueKey = null
        } else {
          trueScope = "global"
          trueKey = key
        }
      }
      val updatedMap = if (trueScope != null && !map.contains(trueScope)) {
        map.updated(trueScope, Map[String, AnyRef]())
      } else map
      elem match {
        case obj: JsonObject =>
          obj.keySet().asScala.map(key => {
            flatten(trueScope, if (trueKey != null) trueKey + "." + key else key, obj.get(key), updatedMap)
          }).fold(updatedMap)((map1, map2) => {
            ConfigurationParser.combineConfigurations(map1, map2)
          })
        case arr: JsonArray =>
          val javaArr = arr.asScala.toArray
          updatedMap.updated(trueScope, updatedMap(trueScope).updated(trueKey, javaArr))
        case _: JsonNull =>
          updatedMap.updated(trueScope, updatedMap(trueScope).updated(trueKey, null))
        case prim: JsonPrimitive =>
          if (prim.isBoolean) {
            updatedMap.updated(trueScope, updatedMap(trueScope).updated(trueKey, prim.getAsBoolean.asInstanceOf[java.lang.Boolean]))
          } else if (prim.isNumber) {
            updatedMap.updated(trueScope, updatedMap(trueScope).updated(trueKey, prim.getAsNumber))
          } else if (prim.isString) {
            updatedMap.updated(trueScope, updatedMap(trueScope).updated(trueKey, prim.getAsString))
          } else {
            updatedMap.updated(trueScope, updatedMap(trueScope).updated(trueKey, null))
          }
        case _ => updatedMap.updated(trueScope, updatedMap(trueScope).updated(trueKey, null))
      }
    }

    LSPConfiguration(flatten(null, null, jsonObject, configMap))
  }
}

class XmlParser extends ConfigurationParser {
  override def parse(file: File): LSPConfiguration = {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(file)
    val root = doc.getDocumentElement
    throw new UnsupportedOperationException
  }
}

class FlatParser extends ConfigurationParser {
  override def parse(file: File): LSPConfiguration = {
    throw new UnsupportedOperationException
  }
}
