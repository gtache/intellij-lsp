package com.github.gtache.lsp.languageserver.definition

import com.github.gtache.lsp.client.connection.ProcessStreamConnectionProvider
import com.github.gtache.lsp.client.connection.StreamConnectionProvider
import com.github.gtache.lsp.utils.CSVLine

/**
 * A base interface for every command-line server definition
 */
interface CommandDefinition : Definition {

    /**
     * The command to run
     */
    val command: List<String>

    override fun createConnectionProvider(directory: String): StreamConnectionProvider {
        return ProcessStreamConnectionProvider(command, directory)
    }

    companion object : DefinitionObject {
        override val presentableType: String = "Command"
        override fun fromMap(map: Map<DefinitionKey, CSVLine>): Definition? {
            throw UnsupportedOperationException("Not available on interface")
        }

        override val type: String = "command"
    }
}