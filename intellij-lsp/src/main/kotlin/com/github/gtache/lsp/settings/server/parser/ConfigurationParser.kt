package com.github.gtache.lsp.settings.server.parser

import com.github.gtache.lsp.settings.server.LSPConfiguration
import com.google.gson.*
import com.intellij.openapi.util.io.FileUtilRt
import java.io.File
import java.io.FileReader
import javax.xml.parsers.DocumentBuilderFactory


interface ConfigurationParser {
    fun parse(file: File): LSPConfiguration

    companion object {
        fun getConfiguration(file: File): LSPConfiguration? {
            val parser = forFile(file)
            return parser?.parse(file)
        }

        fun getConfiguration(doc: String, typ: ConfigType): LSPConfiguration? {
            val parser = forType(typ)
            val file = FileUtilRt.createTempFile("config", "." + ConfigType.toExt(typ), true)
            return parser?.parse(file)
        }


        fun forType(typ: ConfigType?): ConfigurationParser? {
            return when (typ) {
                ConfigType.FLAT -> FlatParser()
                ConfigType.JSON -> JsonParser()
                ConfigType.XML -> XmlParser()
                else -> null
            }
        }

        fun forFile(file: File): ConfigurationParser? {
            val name = file.name
            val idx = name.lastIndexOf('.')
            return if (idx + 1 < name.length) {
                forExt(name.drop(idx + 1))
            } else null
        }

        fun forExt(ext: String): ConfigurationParser? {
            return forType(ConfigType.fromExt(ext))
        }

        fun combineConfigurations(firstConfig: Map<String, Map<String, Any?>>, secondConfig: Map<String, Map<String, Any?>>): Map<String, Map<String, Any?>> {
            val concatMap = HashMap<String, MutableMap<String, Any?>>()
            firstConfig.keys.forEach { key ->
                concatMap[key] = HashMap(firstConfig[key])
            }
            secondConfig.keys.forEach { key ->
                if (concatMap.contains(key)) {
                    secondConfig[key]!!.keys.forEach { subkey -> concatMap[key]!![subkey] = secondConfig[key]!![subkey]!! }
                } else {
                    concatMap[key] = HashMap(secondConfig[key])
                }
            }
            return concatMap.map { pair -> pair.key to pair.value.toMap() }.toMap()
        }
    }
}

class JsonParser : ConfigurationParser {
    override fun parse(file: File): LSPConfiguration {
        val reader = FileReader(file)
        if (file.length() == 0L) {
            return LSPConfiguration.emptyConfiguration
        } else {
            try {
                val json = com.google.gson.JsonParser.parseReader(reader)
                if (json != null && json.isJsonObject) {
                    val jsonObject = json.asJsonObject
                    val configMap = mapOf("global" to HashMap<String, Any>())

                    fun flatten(scope: String?, key: String?, elem: JsonElement, map: Map<String, Map<String, Any?>>): Map<String, Map<String, Any?>> {
                        val trueScope: String
                        val trueKey: String
                        if (scope == null) {
                            if (key == null) {
                                trueScope = "global"
                                trueKey = ""
                            } else if (key.startsWith("<") && key.endsWith(">")) {
                                if (!elem.isJsonObject) {
                                    throw IllegalArgumentException("Check JSON file ${file.absolutePath}")
                                }
                                trueScope = key
                                trueKey = ""
                            } else {
                                trueScope = "global"
                                trueKey = key
                            }
                        } else {
                            trueScope = scope
                            trueKey = key ?: ""
                        }
                        val updatedMap = (if (!map.contains(trueScope)) {
                            map + (trueScope to HashMap())
                        } else map).toMutableMap()
                        val subMap = updatedMap[trueScope]!!
                        when (elem) {
                            is JsonObject ->
                                return elem.keySet().map { k ->
                                    flatten(trueScope, if (trueKey.isNotEmpty()) "$trueKey.$k" else k, elem.get(k), updatedMap)
                                }.fold(updatedMap) { map1, map2 -> ConfigurationParser.combineConfigurations(map1, map2).toMutableMap() }
                            is JsonArray -> {
                                return updatedMap + (trueScope to subMap + (trueKey to elem))
                            }
                            is JsonNull -> return updatedMap + (trueScope to subMap + (trueKey to null))
                            is JsonPrimitive ->
                                return if (elem.isBoolean) {
                                    updatedMap + (trueScope to subMap + (trueKey to elem.asBoolean))
                                } else if (elem.isNumber) {
                                    updatedMap + (trueScope to subMap + (trueKey to elem.asNumber))
                                } else if (elem.isString) {
                                    updatedMap + (trueScope to subMap + (trueKey to elem.asString))
                                } else {
                                    updatedMap + (trueScope to subMap + (trueKey to null))
                                }
                            else -> return updatedMap + (trueScope to subMap + (trueKey to null))
                        }
                    }
                    return LSPConfiguration(flatten("global", null, jsonObject, configMap))
                } else {
                    return LSPConfiguration.invalidConfiguration
                }
            } catch (t: Throwable) {
                return LSPConfiguration.invalidConfiguration
            }
        }
    }
}

class XmlParser : ConfigurationParser {
    override fun parse(file: File): LSPConfiguration {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val root = doc.documentElement
        throw UnsupportedOperationException()
    }
}

class FlatParser : ConfigurationParser {
    override fun parse(file: File): LSPConfiguration {
        throw UnsupportedOperationException()
    }
}