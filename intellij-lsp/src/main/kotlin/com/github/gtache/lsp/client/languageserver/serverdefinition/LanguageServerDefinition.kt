package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.client.LanguageClientImpl
import com.github.gtache.lsp.client.connection.StreamConnectionProvider
import com.intellij.openapi.diagnostic.Logger
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

/**
 * A interface representing a ServerDefinition
 */
interface LanguageServerDefinition {

    companion object {
        private val logger: Logger = Logger.getInstance(LanguageServerDefinition::class.java)

        /**
         * The character to split the extensions with
         */
        const val SPLIT_CHAR: String = ";"

        /**
         * All the language server definitions
         */
        val ALL_DEFINITIONS: MutableSet<LanguageServerDefinition> = HashSet()
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
     * Set of all the extensions that are registered
     */
    val mappedExtensions: MutableSet<String>

    /**
     * Map of working directory -> stream connection provider
     */
    val streamConnectionProviders: MutableMap<String, StreamConnectionProvider>

    /**
     *  The extension that the language server manages
     */
    val ext: String

    /**
     * The id of the language server (same as extension)
     */
    val id: String
        get() = mappedExtensions.map { splitExt -> LanguageIdentifier.extToLanguageId(splitExt) }
            .find { id -> id != null } ?: ext


    /**
     * Starts a Language server for the given working [directory] and returns a tuple (InputStream, OutputStream)
     */
    fun start(directory: String): Pair<InputStream?, OutputStream?> {
        val connectionProvider = streamConnectionProviders[directory]
        return if (connectionProvider != null) {
            Pair(connectionProvider.inputStream, connectionProvider.outputStream)
        } else {
            val newConnectionProvider = createConnectionProvider(directory)
            newConnectionProvider.start()
            streamConnectionProviders[directory] = newConnectionProvider
            return Pair(newConnectionProvider.inputStream, newConnectionProvider.outputStream)
        }
    }

    /**
     * Gets the (output,error) streams pair for the given working [directory]
     */
    fun getOutputStreams(directory: String): Pair<InputStream?, InputStream?>? {
        val connectionProvider = streamConnectionProviders[directory]
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
        val connectionProvider = streamConnectionProviders[directory]
        if (connectionProvider != null) {
            connectionProvider.stop()
            streamConnectionProviders.remove(directory)
        } else {
            logger.warn("No connection for workingDir $directory and ext $ext")
        }
    }

    /**
     * Adds a file [extension] for this definition
     */
    fun addMappedExtension(extension: String): Unit {
        mappedExtensions.addAll(splitExtension(extension))
    }

    /**
     * Removes a file [extension] for this definition
     */
    fun removeMappedExtension(extension: String): Unit {
        mappedExtensions.remove(extension)
    }

    /**
     * Returns the LanguageClient for this LanguageServer
     */
    fun createLanguageClient(): LanguageClientImpl = LanguageClientImpl()

    /**
     * Returns the initialization options for the given [uri]
     */
    fun getInitializationOptions(uri: URI): Any? = null

    /**
     * Returns the string array corresponding to the server definition
     */
    fun toArray(): Array<String> = throw NotImplementedError()

    /**
     * Creates a StreamConnectionProvider given the working [directory]
     */
    fun createConnectionProvider(directory: String): StreamConnectionProvider
}