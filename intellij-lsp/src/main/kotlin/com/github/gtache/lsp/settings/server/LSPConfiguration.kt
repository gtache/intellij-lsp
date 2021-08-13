package com.github.gtache.lsp.settings.server

import com.github.gtache.lsp.settings.server.parser.ConfigurationParser
import java.io.File

data class LSPConfiguration(val settings: Map<String, Map<String, Any?>>) {

    companion object {
        fun fromFile(file: File): LSPConfiguration? {
            return ConfigurationParser.getConfiguration(file)
        }

        //TODO manage User config < Workspace config
        fun fromFiles(files: List<File>): LSPConfiguration {
            val configs = files.map(this::fromFile).mapNotNull { f -> f?.settings }
            return LSPConfiguration(configs.fold(HashMap()) { configTop: Map<String, Map<String, Any?>>, configBottom ->
                ConfigurationParser.combineConfigurations(configTop, configBottom)
            })
        }

        val emptyConfiguration: LSPConfiguration = LSPConfiguration(mapOf(Pair("global", HashMap())))
        val invalidConfiguration: LSPConfiguration = LSPConfiguration(emptyMap())
    }

    fun isValid(): Boolean = this !== invalidConfiguration

    fun getScopeForUri(uri: String): String = "global" //TODO manage different scopes

    fun getAttributesForSectionAndScope(section: String, scope: String = "global"): Map<String, Any?>? {
        return if (isValid()) {
            settings[scope]?.filter { m -> m.key.startsWith(section) }?.map { e ->
                val strippedKey = e.key.removePrefix(section)
                if (strippedKey.startsWith(".")) {
                    Pair(strippedKey.removePrefix("."), e.value)
                } else {
                    Pair(strippedKey, e.value)
                }
            }?.toMap()
        } else null
    }


    fun getAttributesForSectionAndUri(section: String, uri: String): Map<String, Any?>? {
        return getAttributesForSectionAndScope(section, getScopeForUri(uri))
    }
}