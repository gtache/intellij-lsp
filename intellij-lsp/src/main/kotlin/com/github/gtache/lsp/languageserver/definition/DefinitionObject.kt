package com.github.gtache.lsp.languageserver.definition

import com.github.gtache.lsp.utils.CSVLine

/**
 * Trait for companion objects of the UserConfigurableServerDefinition classes
 */
interface DefinitionObject {
    /**
     * The type of the server definition
     */
    val type: String

    /**
     * The type of the server definition in a nicer, human-readable way
     */
    val presentableType: String

    /**
     * Returns the server definition defined by the given [map] of attributes
     */
    fun fromMap(map: Map<DefinitionKey, CSVLine>): Definition?
}