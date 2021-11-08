package com.github.gtache.lsp.client.languageserver.serverdefinition

/**
 * A UserConfigurableServerDefinition is a server definition which can be manually entered by the user in the IntellliJ settings
 */
interface UserConfigurableServerDefinition : LanguageServerDefinition {

    companion object : UserConfigurableServerDefinitionObject {

        /**
         * Transforms a [map] of <String, UserConfigurableServerDefinition> to a Map<String, Array<String>>
         */
        fun toArrayMap(map: Map<String, UserConfigurableServerDefinition>): Map<String, Array<String>> {
            return map.mapValues { e -> e.value.toArray() }
        }

        /**
         * Transforms a [map] of <String, Array<String>> to a Map<String, UserConfigurableServerDefinition>
         */
        fun fromArrayMap(map: Map<String, Array<String>>): Map<String, UserConfigurableServerDefinition> {
            return map.mapValues { e -> fromArray(e.value) }.filterValues { v -> v != null }.mapValues { e -> e.value as UserConfigurableServerDefinition }
        }

        override fun fromArray(arr: Array<String>): UserConfigurableServerDefinition? {
            val filteredArr = arr.filter { s -> s.trim() != "" }.toTypedArray()
            return ArtifactLanguageServerDefinition.fromArray(filteredArr) ?: CommandServerDefinition.fromArray(filteredArr)
        }

        override val type: String = "userConfigurable"
        override val presentableType: String = "Configurable"
    }
}