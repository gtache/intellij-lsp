package com.github.gtache.lsp.languageserver.configuration

import com.github.gtache.lsp.languageserver.configuration.provider.ConfigurationProvider
import com.intellij.openapi.components.service

/**
 * Represents an LSP Configuration for a specific server
 * @constructor The [settings]
 */
data class ConfigurationImpl(override val settings: Map<String, Map<String, Any?>>) : Configuration {

    override fun isValid(): Boolean {
        return this !== service<ConfigurationProvider>().getInvalidConfiguration()
    }

    override fun getScopeForUri(uri: String): String = "global" //TODO manage different scopes

    override fun getAttributesForSectionAndScope(section: String, scope: String): Map<String, Any?>? {
        return if (isValid()) {
            settings[scope]?.filter { (key) -> key.startsWith(section) }?.map { e ->
                val strippedKey = e.key.removePrefix(section)
                if (strippedKey.startsWith(".")) {
                    Pair(strippedKey.removePrefix("."), e.value)
                } else {
                    Pair(strippedKey, e.value)
                }
            }?.toMap()
        } else null
    }

}