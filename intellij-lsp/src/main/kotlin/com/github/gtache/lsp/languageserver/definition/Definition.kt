package com.github.gtache.lsp.languageserver.definition

import com.github.gtache.lsp.client.LanguageClientImpl
import com.github.gtache.lsp.client.connection.StreamConnectionProvider
import com.github.gtache.lsp.utils.CSVLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

/**
 * A interface representing a ServerDefinition
 */
interface Definition {

    companion object {
        private val logger: Logger = Logger.getInstance(Definition::class.java)
        val EP_NAME: ExtensionPointName<Definition> = ExtensionPointName.create("com.github.gtache.lsp.lspDefinition")

        /**
         * The character to split the extensions with
         */
        const val SPLIT_CHAR: String = ";"

        /**
         * All the language server definitions
         */
        val ALL_DEFINITIONS: MutableSet<Definition> = HashSet()
            get() = HashSet(field)

        /**
         * Splits an [extension] string into a list of extensions
         */
        fun splitExtension(extension: String): List<String> {
            val split = extension.split(SPLIT_CHAR)
            return if (split.size > 1) {
                val tmp = split.toMutableList()
                tmp.add(extension)
                return tmp
            } else {
                listOf(extension)
            }
        }
    }

    /**
     * The id of the language server
     */
    val id: String

    /**
     * The Stream connection provider
     */
    val streamConnectionProvider: MutableMap<String, StreamConnectionProvider>

    /**
     *  The extensions that the language server manages
     */
    val extensions: Set<String>

    /**
     * Starts a Language server for the given working [directory] and returns a tuple (InputStream, OutputStream)
     */
    fun start(directory: String): Pair<InputStream?, OutputStream?> {
        val connectionProvider = streamConnectionProvider[directory]
        return if (connectionProvider != null) {
            Pair(connectionProvider.inputStream, connectionProvider.outputStream)
        } else {
            val newConnectionProvider = createConnectionProvider(directory)
            newConnectionProvider.start()
            streamConnectionProvider[directory] = newConnectionProvider
            return Pair(newConnectionProvider.inputStream, newConnectionProvider.outputStream)
        }
    }

    /**
     * Gets the (output,error) streams pair for the given working [directory]
     */
    fun getOutputStreams(directory: String): Pair<InputStream?, InputStream?>? {
        val connectionProvider = streamConnectionProvider[directory]
        return if (connectionProvider != null) {
            Pair(connectionProvider.inputStream, connectionProvider.errorStream)
        } else {
            logger.warn("Trying to get streams of un-started process")
            null
        }
    }

    /**
     * Stops the Language server corresponding to the given working [directory]
     */
    fun stop(directory: String): Unit {
        val connectionProvider = streamConnectionProvider[directory]
        if (connectionProvider != null) {
            connectionProvider.stop()
            streamConnectionProvider.remove(directory)
        } else {
            logger.warn("No connection for workingDir $directory and id $id")
        }
    }

    /**
     * Adds a file [extension] for this definition
     */
    fun addExtension(extension: String)

    /**
     * Removes a file [extension] for this definition
     */
    fun removeExtension(extension: String)

    /**
     * Returns the LanguageClient for this LanguageServer
     */
    fun createLanguageClient(): LanguageClientImpl = LanguageClientImpl()

    /**
     * Returns the initialization options for the given [uri]
     */
    fun getInitializationOptions(uri: URI): Any? = null

    /**
     * Creates a StreamConnectionProvider given the working [directory]
     */
    fun createConnectionProvider(directory: String): StreamConnectionProvider

    /**
     * Gets the mapping of attributes of this definition
     */
    fun toMap(): Map<DefinitionKey, CSVLine>
}