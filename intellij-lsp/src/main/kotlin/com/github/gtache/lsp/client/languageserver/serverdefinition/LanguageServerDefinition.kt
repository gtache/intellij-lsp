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
        const val SPLIT_CHAR = ";"
        val allDefinitions: MutableSet<LanguageServerDefinition> = HashSet()
            get() = HashSet(field)
        private val logger: Logger = Logger.getInstance(LanguageServerDefinition::class.java)

        /**
         * Register a server definition
         *
         * @param definition The server definition
         */
        fun register(definition: LanguageServerDefinition): Unit {
            allDefinitions.add(definition)
            logger.info("Added definition for $definition")
        }

        fun fromArray(arr: Array<String>): LanguageServerDefinition? {
            return UserConfigurableServerDefinition.fromArray(arr)
        }
    }

    val mappedExtensions: MutableSet<String>
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
     * Starts a Language server for the given directory and returns a tuple (InputStream, OutputStream)
     *
     * @param workingDir The root directory
     * @return The input and output streams of the server
     */
    fun start(workingDir: String): Pair<InputStream?, OutputStream?> {
        val connectionProvider = streamConnectionProviders[workingDir]
        return if (connectionProvider != null) {
            Pair(connectionProvider.inputStream, connectionProvider.outputStream)
        } else {
            val newConnectionProvider = createConnectionProvider(workingDir)
            newConnectionProvider.start()
            streamConnectionProviders[workingDir] = newConnectionProvider
            return Pair(newConnectionProvider.inputStream, newConnectionProvider.outputStream)
        }
    }

    fun getOutputStreams(workingDir: String): Pair<InputStream?, InputStream?>? {
        val connectionProvider = streamConnectionProviders[workingDir]
        return if (connectionProvider != null) {
            Pair(connectionProvider.inputStream, connectionProvider.errorStream)
        } else {
            logger.warn("Trying to get streams of unstarted process")
            null
        }
    }

    /**
     * Stops the Language server corresponding to the given working directory
     *
     * @param workingDir The root directory
     */
    fun stop(workingDir: String): Unit {
        val connectionProvider = streamConnectionProviders[workingDir]
        if (connectionProvider != null) {
            connectionProvider.stop()
            streamConnectionProviders.remove(workingDir)
        } else {
            logger.warn("No connection for workingDir $workingDir and ext $ext")
        }
    }

    /**
     * Adds a file extension for this LanguageServer
     *
     * @param ext the extension
     */
    fun addMappedExtension(ext: String): Unit {
        mappedExtensions.addAll(splitExtension(ext))
    }

    fun splitExtension(ext: String): List<String> {
        val split = ext.split(";")
        return if (split.size > 1) {
            val tmp = split.toMutableList()
            tmp.add(ext)
            return tmp
        } else {
            listOf(ext)
        }
    }

    /**
     * Removes a file extension for this LanguageServer
     *
     * @param ext the extension
     */
    fun removeMappedExtension(ext: String): Unit {
        mappedExtensions.remove(ext)
    }

    /**
     * @return the LanguageClient for this LanguageServer
     */
    fun createLanguageClient(): LanguageClientImpl = LanguageClientImpl()

    fun getInitializationOptions(uri: URI): Any? = null

    /**
     * @return The array corresponding to the server definition
     */
    fun toArray(): Array<String> = throw NotImplementedError()

    /**
     * Creates a StreamConnectionProvider given the working directory
     *
     * @param workingDir The root directory
     * @return The stream connection provider
     */
    fun createConnectionProvider(workingDir: String): StreamConnectionProvider
}