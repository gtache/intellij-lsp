package com.github.gtache.lsp.client.languageserver.wrapper

import com.github.gtache.lsp.client.languageserver.status.ServerStatus
import com.github.gtache.lsp.client.languageserver.requestmanager.RequestManager
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.settings.server.Configuration
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.FileChangeType
import org.eclipse.lsp4j.RegistrationParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.UnregistrationParams
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture

/**
 * A LanguageServerWrapper represents a connection to a LanguageServer and manages starting / stopping it as well as  connecting / disconnecting documents to it
 */
interface LanguageServerWrapper {

    /**
     * The server definition corresponding to this wrapper
     */
    val serverDefinition: LanguageServerDefinition

    /**
     * The project corresponding to this wrapper
     */
    val project: Project

    /**
     * The current status of this server
     */
    val status: ServerStatus

    /**
     * The request manager for this wrapper
     */
    val requestManager: RequestManager?

    /**
     * The language server
     */
    val languageServer: LanguageServer?

    /**
     * The language server configuration
     */
    var configuration: Configuration?

    /**
     * Tells the wrapper if a request was timed out or not
     *
     * @param timeouts The type of timeout
     * @param success  if it didn't timeout
     */
    fun notifyResult(timeouts: Timeouts, success: Boolean): Unit

    fun notifySuccess(timeouts: Timeouts): Unit = notifyResult(timeouts, success = true)

    fun notifyFailure(timeouts: Timeouts): Unit = notifyResult(timeouts, success = false)

    /**
     * Register a capability for the language server
     *
     * @param params The registration params
     * @return
     */
    fun registerCapability(params: RegistrationParams): CompletableFuture<Void>

    /**
     * Unregister a capability for the language server
     *
     * @param params The unregistration params
     * @return
     */
    fun unregisterCapability(params: UnregistrationParams): CompletableFuture<Void>

    /**
     * Returns the EditorEventManager for a given uri
     *
     * @param uri the URI as a string
     * @return the EditorEventManager (or null)
     */
    fun getEditorManagerFor(uri: String): EditorEventManager?

    /**
     * Starts the LanguageServer
     */
    fun start(): Unit

    /**
     * Connects an editor to the languageServer
     *
     * @param editor the editor
     */
    fun connect(editor: Editor): Unit

    /**
     * Disconnects an editor from the LanguageServer
     *
     * @param editor The editor
     */
    fun disconnect(editor: Editor): Unit

    /**
     * Disconnects an editor from the LanguageServer
     *
     * @param path The uri of the editor
     */
    fun disconnect(path: String): Unit

    /**
     * Checks if the wrapper is already connected to the document at the given path
     */
    fun isConnectedTo(location: String): Boolean

    /**
     * @return The language ID that this wrapper is dealing , if defined in the content type mapping for the language languageServer
     */
    fun getLanguageId(contentTypes: Array<String>): String?

    fun logMessage(message: Message): Unit

    /**
     * Stops the wrapper
     */
    fun stop(): Unit

    /**
     * Notifies the wrapper that the server has crashed / stopped unexpectedly
     *
     * @param e The exception returned
     */
    fun crashed(e: Exception): Unit

    fun removeWidget(): Unit

    fun restart(): Unit

    fun didChangeWatchedFiles(uri: String, typ: FileChangeType): Unit

    /**
     * @return whether the underlying connection to language languageServer is still active
     */
    fun isActive(): Boolean

    /**
     * Gets the list of currently connected files
     */
    fun getConnectedFiles(): Iterable<String>

    /**
     * @return The server capabilities
     */
    fun getServerCapabilities(): ServerCapabilities?
    fun getServer(): LanguageServer?
}