package com.github.gtache.lsp.client.languageserver.serverdefinition

/**
 * Trait for companion objects of the UserConfigurableServerDefinition classes
 */
interface UserConfigurableServerDefinitionObject {
    /**
     * The type of the server definition
     */
    val type: String

    /**
     * The type of the server definition in a nicer, human-readable way
     */
    val presentableType: String

    /**
     * Transforms an array of string into the corresponding UserConfigurableServerDefinition
     *
     * @param arr The array
     * Returns The server definition
     */
    fun fromArray(arr: Array<String>): UserConfigurableServerDefinition?
}