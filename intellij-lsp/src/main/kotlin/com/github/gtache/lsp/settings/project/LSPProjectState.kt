package com.github.gtache.lsp.settings.project

import com.github.gtache.lsp.filterNotNullValues
import com.github.gtache.lsp.languageserver.definition.Definition
import com.github.gtache.lsp.languageserver.definition.provider.LanguageServerDefinitionProvider
import com.github.gtache.lsp.languageserver.settings.Settings
import com.github.gtache.lsp.settings.project.converters.AdvancedDefinitionSettingsConverter
import com.github.gtache.lsp.utils.CSVLine
import com.intellij.openapi.components.service

/**
 * Represents the current project lsp settings.
 * Don't change the values manually, use the "with" methods and set the value in LSPPersistentProjectSettings. Public setters are required for saving.
 */
data class LSPProjectState(
    var idToDefinitionString: Map<String, Map<String, String>>,
    var forcedAssociations: Map<String, String>,
    var idToSettingsString: Map<String, String>,
) {
    companion object {
        private val settingsConverter = AdvancedDefinitionSettingsConverter()
        private val serverDefinitionProvider = service<LanguageServerDefinitionProvider>()
    }

    val idToDefinition = idToDefinitionString.mapValues { (_, v) -> serverDefinitionProvider.getLanguageServerDefinition(v.mapValues { (_, s) -> CSVLine(s) }) }
        .filterNotNullValues()
    val idToSettings = idToSettingsString.mapValues { (_, v) -> settingsConverter.fromString(v, null) }.filterNotNullValues()

    /**
     * Map of file extension to server definition
     */
    val extensionToServerDefinition: Map<String, Definition> = if (idToDefinition.isEmpty()) emptyMap() else idToDefinition.values.map { v -> v.extensions.associateWith { v } }
        .reduce { acc, map -> acc.plus(map) }

    constructor() : this(emptyMap(), emptyMap(), emptyMap())

    /**
     * Returns a new State with the new [idToDefinition] values
     */
    fun withDefinitions(idToDefinition: Map<String, Map<String, String>>): LSPProjectState {
        return LSPProjectState(idToDefinition, forcedAssociations, idToSettingsString)
    }

    /**
     * Returns a new State with the new [idToDefinition] values
     */
    fun withObjectDefinitions(idToDefinition: Map<String, Definition>): LSPProjectState {
        val idToDefinitionString = idToDefinition.mapValues { (_, v) -> v.toMap().mapKeys { (k, _) -> k.toString() }.mapValues { (_, l) -> l.csvLine } }
        return LSPProjectState(idToDefinitionString, forcedAssociations, idToSettingsString)
    }

    /**
     * Returns a new State with the new [forcedAssociations] values
     */
    fun withForcedAssociations(forcedAssociations: Map<String, String>): LSPProjectState {
        return LSPProjectState(idToDefinitionString, forcedAssociations, idToSettingsString)
    }

    /**
     * Returns a new State with the new [idToSettings] values
     */
    fun withIdToSettings(idToSettings: Map<String, String>): LSPProjectState {
        return LSPProjectState(idToDefinitionString, forcedAssociations, idToSettings)
    }

    /**
     * Returns a new State with the new [idToSettings] values
     */
    fun withObjectIdToSettings(idToSettings: Map<String, Settings>): LSPProjectState {
        val idToSettingsString = idToSettings.mapValues { (_, v) -> settingsConverter.toString(v, null) }.filterNotNullValues()
        return LSPProjectState(idToDefinitionString, forcedAssociations, idToSettingsString)
    }
}