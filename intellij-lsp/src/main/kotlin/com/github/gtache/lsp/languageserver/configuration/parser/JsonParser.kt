package com.github.gtache.lsp.languageserver.configuration.parser

import com.github.gtache.lsp.languageserver.configuration.Configuration
import com.github.gtache.lsp.languageserver.configuration.ConfigurationImpl
import com.github.gtache.lsp.languageserver.configuration.provider.ConfigurationProvider
import com.google.gson.*
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger

/**
 * Configuration parser for JSON
 */
class JsonParser : ConfigurationParser {

    companion object {
        private val logger: Logger = Logger.getInstance(JsonParser::class.java)
    }

    override fun parse(document: String): Configuration {
        if (document.isEmpty()) {
            return service<ConfigurationProvider>().getEmptyConfiguration()
        } else {
            try {
                val json = com.google.gson.JsonParser.parseString(document)
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
                                    throw IllegalArgumentException("Check JSON document $document")
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
                                }.fold(updatedMap) { map1, map2 -> service<ConfigurationProvider>().combineConfigurations(map1, map2).toMutableMap() }
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
                    return ConfigurationImpl(flatten("global", null, jsonObject, configMap))
                } else {
                    return service<ConfigurationProvider>().getInvalidConfiguration()
                }
            } catch (t: Throwable) {
                logger.warn("Error parsing JSON", t)
                return service<ConfigurationProvider>().getInvalidConfiguration()
            }
        }
    }
}