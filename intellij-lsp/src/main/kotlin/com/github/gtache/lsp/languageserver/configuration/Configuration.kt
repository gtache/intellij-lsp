package com.github.gtache.lsp.languageserver.configuration

/**
 * Represents a language server configuration (defined by the language server itself)
 */
sealed interface Configuration {

    /**
     * The whole settings
     */
    val settings: Map<String, Map<String, Any?>>

    /**
     * Returns whether this configuration is valid or not
     */
    fun isValid(): Boolean

    /**
     * Returns the scope for a given [uri]
     */
    fun getScopeForUri(uri: String): String

    /**
     * Returns the settings / attributes for a [section] and a [scope]
     */
    fun getAttributesForSectionAndScope(section: String, scope: String = "global"): Map<String, Any?>?

    /**
     * Returns the settings / attributes for a [section] and an [uri]
     */
    fun getAttributesForSectionAndUri(section: String, uri: String): Map<String, Any?>? {
        return getAttributesForSectionAndScope(section, getScopeForUri(uri))
    }
}