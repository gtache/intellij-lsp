package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.client.connection.ProcessStreamConnectionProvider
import com.github.gtache.lsp.client.connection.StreamConnectionProvider

/**
 * A base interface for every command-line server definition
 */
interface CommandServerDefinition : UserConfigurableServerDefinition {

    /**
     * The command to run
     */
    val command: Array<String>

    override fun createConnectionProvider(directory: String): StreamConnectionProvider {
        return ProcessStreamConnectionProvider(command, directory)
    }

    companion object : UserConfigurableServerDefinitionObject {
        override val presentableType: String = "Command"
        override val type: String = "command"

        override fun fromArray(arr: Array<String>): UserConfigurableServerDefinition? {
            return RawCommandServerDefinition.fromArray(arr) ?: ExeLanguageServerDefinition.fromArray(arr)
        }
    }
}