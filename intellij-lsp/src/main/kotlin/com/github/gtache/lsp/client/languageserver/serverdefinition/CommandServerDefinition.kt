package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.client.connection.ProcessStreamConnectionProvider
import com.github.gtache.lsp.client.connection.StreamConnectionProvider

/**
 * A base interface for every command-line server definition
 */
interface CommandServerDefinition : UserConfigurableServerDefinition {

    val command: Array<String>

    override fun createConnectionProvider(workingDir: String): StreamConnectionProvider {
        return ProcessStreamConnectionProvider(command, workingDir)
    }

    companion object : UserConfigurableServerDefinitionObject {
        override val presentableTyp: String = "Command"
        override val typ = "command"

        override fun fromArray(arr: Array<String>): UserConfigurableServerDefinition? {
            return RawCommandServerDefinition.fromArray(arr) ?: ExeLanguageServerDefinition.fromArray(arr)
        }
    }
}