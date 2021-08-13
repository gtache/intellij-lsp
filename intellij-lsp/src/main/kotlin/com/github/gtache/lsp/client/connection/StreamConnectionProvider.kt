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

    val inputStream: InputStream?
    val outputStream: OutputStream?
    val errorStream: InputStream?

    fun start(): Unit

    /**
     * User provided initialization options.
     */
    fun getInitializationOptions(rootUri: URI): Any? = null

    fun stop(): Unit

    /**
     * Allows to hook custom behavior on messages.
     *
     * @param message        a message
     * @param languageServer the language server receiving/sending the message.
     * @param rootURI
     */
    fun handleMessage(message: Message, languageServer: LanguageServer, rootURI: URI): Unit {
    }
}