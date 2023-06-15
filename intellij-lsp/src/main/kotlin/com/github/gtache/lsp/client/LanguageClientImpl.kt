package com.github.gtache.lsp.client

import com.github.gtache.lsp.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.editor.services.project.EditorProjectService
import com.github.gtache.lsp.requests.SemanticHighlightingHandler
import com.github.gtache.lsp.requests.WorkspaceEditHandler
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.UIUtil
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.FutureTask


/**
 * Implementation of the LanguageClient
 */
class LanguageClientImpl : LanguageClient {

    companion object {
        private val logger: Logger = Logger.getInstance(LanguageClientImpl::class.java)
    }

    private var server: LanguageServer? = null
    private var wrapper: LanguageServerWrapper? = null

    /**
     * Connects the LanguageClient to the server
     *
     * @param server The LanguageServer
     */
    fun connect(server: LanguageServer, wrapper: LanguageServerWrapper): Unit {
        this.server = server
        this.wrapper = wrapper
    }

    override fun applyEdit(params: ApplyWorkspaceEditParams): CompletableFuture<ApplyWorkspaceEditResponse> {
        val project = wrapper?.project
        return if (project != null) {
            CompletableFuture.supplyAsync {
                ApplyWorkspaceEditResponse(WorkspaceEditHandler.applyEdit(params.edit, project))
            }
        } else {
            logger.warn("Null project for $this")
            CompletableFuture.completedFuture(ApplyWorkspaceEditResponse(false))
        }
    }

    override fun configuration(configurationParams: ConfigurationParams): CompletableFuture<List<Any>> {
        val config = wrapper?.configuration
        return CompletableFuture.completedFuture(configurationParams.items.mapNotNull { ci ->
            val uri = ci.scopeUri
            val section = ci.section
            config?.getAttributesForSectionAndUri(section, uri)
        })
    }

    override fun registerCapability(params: RegistrationParams): CompletableFuture<Void>? = wrapper?.registerCapability(params)

    override fun unregisterCapability(params: UnregistrationParams): CompletableFuture<Void>? = wrapper?.unregisterCapability(params)

    override fun telemetryEvent(o: Any): Unit {
        //TODO
    }

    override fun publishDiagnostics(publishDiagnosticsParams: PublishDiagnosticsParams): Unit {
        val uri = FileUtils.sanitizeURI(publishDiagnosticsParams.uri)
        val diagnostics = publishDiagnosticsParams.diagnostics
        wrapper?.project?.service<EditorProjectService>()?.forUri(uri)?.diagnostics(diagnostics)
    }

    override fun showMessage(messageParams: MessageParams): Unit {
        val title = "Language Server message"
        val message = messageParams.message
        ApplicationUtils.invokeLater {
            when (messageParams.type) {
                MessageType.Error -> Messages.showErrorDialog(message, title)
                MessageType.Warning -> Messages.showWarningDialog(message, title)
                MessageType.Info -> Messages.showInfoMessage(message, title)
                MessageType.Log -> Messages.showInfoMessage(message, title)
                else -> logger.warn("No message type for $message")
            }
        }
    }

    override fun showMessageRequest(showMessageRequestParams: ShowMessageRequestParams): CompletableFuture<MessageActionItem> {
        val actions = showMessageRequestParams.actions
        val title = "Language Server message"
        val message = showMessageRequestParams.message
        val icon = when (showMessageRequestParams.type) {
            MessageType.Error -> UIUtil.getErrorIcon()
            MessageType.Warning -> UIUtil.getWarningIcon()
            MessageType.Info -> UIUtil.getInformationIcon()
            MessageType.Log -> UIUtil.getInformationIcon()
            else -> {
                logger.warn("No message type for $message")
                null
            }
        }

        val task = FutureTask { Messages.showDialog(message, title, actions.map { a -> a.title }.toTypedArray(), 0, icon) }
        ApplicationManager.getApplication().invokeAndWait(task)
        val exitCode = task.get()

        return CompletableFuture.completedFuture(MessageActionItem(actions[exitCode].title))
    }

    override fun logMessage(messageParams: MessageParams): Unit {
        val message = messageParams.message
        when (messageParams.type) {
            MessageType.Error ->
                logger.warn("ERROR\n$message") //logger.error will throw an error notification in IntelliJ, which is not desired
            MessageType.Warning ->
                logger.warn(message)
            MessageType.Info ->
                logger.info(message)
            MessageType.Log ->
                logger.debug(message)
            else ->
                logger.warn("Unknown message type for $message")
        }
    }

    override fun semanticHighlighting(params: SemanticHighlightingParams): Unit {
        val project = wrapper?.project
        if (project != null) {
            SemanticHighlightingHandler.handlePush(params, project)
        }
    }
}