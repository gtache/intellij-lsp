package com.github.gtache.lsp.settings.server

import com.github.gtache.lsp.settings.server.parser.ConfigurationParser
import java.io.File

/**
 * Represents an LSP Configuration for a specific server
 * @constructor The [settings]
 */
data class Configuration(val settings: Map<String, Map<String, Any?>>) {

    companion object {
        /**
         * Returns a configuration given a [file]
         */
        fun fromFile(file: File): Configuration? {
            return ConfigurationParser.getConfiguration(file)
        }

        //TODO manage User config < Workspace config
        /**
         * Returns a Configuration given multiple [files]
         */
        fun fromFiles(files: List<File>): Configuration {
            val configs = files.map(this::fromFile).mapNotNull { f -> f?.settings }
            return Configuration(configs.fold(HashMap()) { configTop: Map<String, Map<String, Any?>>, configBottom ->
                ConfigurationParser.combineConfigurations(configTop, configBottom)
            })
        }

        /**
         * An empty configuration
         */
        val EMPTY_CONFIGURATION: Configuration = Configuration(mapOf(Pair("global", HashMap())))

        /**
         * An invalid configuration
         */
        val INVALID_CONFIGURATION: Configuration = Configuration(emptyMap())
    }

    /**
     * Returns whether this configuration is valid or not
     */
    fun isValid(): Boolean = this !== INVALID_CONFIGURATION

    /**
     * Returns the scope for a given [uri]
     */
    fun getScopeForUri(uri: String): String = "global" //TODO manage different scopes

    /**
     * Returns the settings / attributes for a [section] and a [scope]
     */
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


    /**
     * Returns the settings / attributes for a [section] and an [uri]
     */
    fun getAttributesForSectionAndUri(section: String, uri: String): Map<String, Any?>? {
        return getAttributesForSectionAndScope(section, getScopeForUri(uri))
    }
}