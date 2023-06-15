package com.github.gtache.lsp.languageserver.definition.provider

import com.github.gtache.lsp.languageserver.definition.Definition
import com.github.gtache.lsp.utils.CSVLine

/**
 * Provides Language Server Definitions given map or list representations of them
 */
interface LanguageServerDefinitionProvider {
    /**
     * Gets the server definition for the given map
     */
    fun getLanguageServerDefinition(map: Map<String, CSVLine>): Definition?

    /**
     * Gets the server definition for the given list
     */
    fun getLanguageServerDefinition(list: List<CSVLine>): Definition?
}