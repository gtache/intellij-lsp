package com.github.gtache.lsp.client.languageserver.wrapper

import com.github.gtache.lsp.client.languageserver.requestmanager.RequestManager
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.client.languageserver.status.ServerStatus
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
     * Tells the wrapper if a request [timeout] was timed out or not ([success])
     */
    fun notifyResult(timeout: Timeouts, success: Boolean): Unit

    /**
     * Notifies the wrapper of a non-[timeout]
     */
    fun notifySuccess(timeout: Timeouts): Unit = notifyResult(timeout, success = true)

    /**
     * Notifies the wrapper of a [timeout]
     */
    fun notifyFailure(timeout: Timeouts): Unit = notifyResult(timeout, success = false)

    /**
     * Registers a capability defined by the registration [params] with the language server
     */
    fun registerCapability(params: RegistrationParams): CompletableFuture<Void>

    /**
     * Unregisters a capability defined by the un-registration [params] with the language server
     */
    fun unregisterCapability(params: UnregistrationParams): CompletableFuture<Void>

    /**
     * Returns the EditorEventManager for a given [uri]
     */
    fun getEditorManagerFor(uri: String): EditorEventManager?

    /**
     * Starts the LanguageServer
     */
    fun start(): Unit

    /**
     * Connects an [editor] to the languageServer
     */
    fun connect(editor: Editor): Unit

    /**
     * Disconnects an [editor] from the LanguageServer
     */
    fun disconnect(editor: Editor): Unit

    /**
     * Disconnects an editor corresponding to the given [path] from the LanguageServer
     */
    fun disconnect(path: String): Unit

    /**
     * Checks if the wrapper is already connected to the document at the given [location]
     */
    fun isConnectedTo(location: String): Boolean

    /**
     * Returns the language ID that this wrapper is managing, if defined in the [contentTypes] mapping
     */
    fun getLanguageId(contentTypes: Array<String>): String?

    /**
     * Tells the wrapper to log a [message] sent by the server
     */
    fun logMessage(message: Message): Unit

    /**
     * Stops the wrapper
     */
    fun stop(): Unit

    /**
     * Notifies the wrapper that the server has crashed / stopped unexpectedly due to the given [exception]
     */
    fun crashed(exception: Exception): Unit

    /**
     * Removes the widget for this server
     */
    fun removeWidget(): Unit

    /**
     * Restarts the server
     */
    fun restart(): Unit

    /**
     * Notifies that an event of [type] happened to the file corresponding to the given [uri]
     */
    fun didChangeWatchedFiles(uri: String, type: FileChangeType): Unit

    /**
     * Returns whether the underlying connection to language languageServer is still active
     */
    fun isActive(): Boolean

    /**
     * Returns the list of currently connected files
     */
    fun getConnectedFiles(): Iterable<String>

    /**
     * Returns the server capabilities
     */
    fun getServerCapabilities(): ServerCapabilities?

    /**
     * Returns the language server object
     */
    fun getServer(): LanguageServer?
}