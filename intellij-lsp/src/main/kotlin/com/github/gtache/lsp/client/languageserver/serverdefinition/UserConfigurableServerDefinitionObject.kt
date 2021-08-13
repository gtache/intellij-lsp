package com.github.gtache.lsp.client.languageserver.serverdefinition

/**
 * Trait for companion objects of the UserConfigurableServerDefinition classes
 */
interface UserConfigurableServerDefinitionObject {
    /**
     * The type of the server definition
     */
    val typ: String

    /**
     * The type of the server definition in a nicer way
     */
    val presentableTyp: String

    /**
     * Transforms an array of string into the corresponding UserConfigurableServerDefinition
     *
     * @param arr The array
     * @return The server definition
     */
    fun fromArray(arr: Array<String>): UserConfigurableServerDefinition?
}