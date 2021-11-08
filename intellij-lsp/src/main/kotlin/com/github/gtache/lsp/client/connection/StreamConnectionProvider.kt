/* Adapted from lsp4e */
package com.github.gtache.lsp.client.connection

import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.services.LanguageServer
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

/**
 * A class representing a stream connection
 */
interface StreamConnectionProvider {

    /**
     * The connection input stream
     */
    val inputStream: InputStream?

    /**
     * The connection output stream
     */
    val outputStream: OutputStream?

    /**
     * The connection error stream
     */
    val errorStream: InputStream?

    /**
     * Starts the connection
     */
    fun start(): Unit

    /**
     * User provided initialization options.
     */
    fun getInitializationOptions(rootUri: URI): Any? = null

    /**
     * Stops the connection
     */
    fun stop(): Unit

    /**
     * Handles the given [message] sent/received by the given [languageServer] at the [rootURI]
     */
    fun handleMessage(message: Message, languageServer: LanguageServer, rootURI: URI): Unit {
    }
}