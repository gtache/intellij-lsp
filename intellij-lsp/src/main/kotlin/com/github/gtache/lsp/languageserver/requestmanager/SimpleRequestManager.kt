package com.github.gtache.lsp.languageserver.requestmanager

import com.github.gtache.lsp.languageserver.status.ServerStatus
import com.github.gtache.lsp.languageserver.wrapper.LanguageServerWrapper
import com.intellij.openapi.diagnostic.Logger
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.CancelParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture

/**
 * Basic implementation of a RequestManager which just passes requests from client to server and vice-versa
 */
class SimpleRequestManager(
    private val wrapper: LanguageServerWrapper,
    private val server: LanguageServer,
    private val client: LanguageClient,
    private val serverCapabilities: ServerCapabilities
) : RequestManager {

    private val textDocumentOptions =
        if (serverCapabilities.textDocumentSync.isRight) serverCapabilities.textDocumentSync.right else null

    private val workspaceService: WorkspaceService = server.workspaceService
    private val textDocumentService: TextDocumentService = server.textDocumentService

    companion object {
        private val logger: Logger = Logger.getInstance(SimpleRequestManager::class.java)
    }

    //Client
    override fun showMessage(messageParams: MessageParams): Unit = client.showMessage(messageParams)


    override fun showMessageRequest(showMessageRequestParams: ShowMessageRequestParams): CompletableFuture<MessageActionItem> =
        client.showMessageRequest(showMessageRequestParams)


    override fun logMessage(messageParams: MessageParams): Unit = client.logMessage(messageParams)


    override fun telemetryEvent(o: Any): Unit = client.telemetryEvent(o)

    override fun registerCapability(params: RegistrationParams): CompletableFuture<Void> =
        client.registerCapability(params)

    override fun unregisterCapability(params: UnregistrationParams): CompletableFuture<Void> =
        client.unregisterCapability(params)

    override fun applyEdit(params: ApplyWorkspaceEditParams): CompletableFuture<ApplyWorkspaceEditResponse> =
        client.applyEdit(params)

    override fun publishDiagnostics(publishDiagnosticsParams: PublishDiagnosticsParams): Unit =
        client.publishDiagnostics(publishDiagnosticsParams)

    override fun configuration(configurationParams: ConfigurationParams): CompletableFuture<List<Any>> =
        client.configuration(configurationParams)

    override fun semanticHighlighting(params: SemanticHighlightingParams): Unit = client.semanticHighlighting(params)

    //General
    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult>? {
        return if (checkStatus()) try {
            server.initialize(params)
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun initialized(params: InitializedParams): Unit {
        if (checkStatus()) try {
            server.initialized(params)
        } catch (e: Exception) {
            crashed(e)
        }
    }

    override fun shutdown(): CompletableFuture<Any>? {
        return if (checkStatus()) try {
            server.shutdown()
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun exit(): Unit {
        if (checkStatus()) try {
            server.exit()
        } catch (e: Exception) {
            crashed(e)
        }
    }

    override fun cancelRequest(params: CancelParams): Unit {
    }

    //Workspace
    override fun didChangeConfiguration(params: DidChangeConfigurationParams): Unit {
        if (checkStatus()) try {
            workspaceService.didChangeConfiguration(params)
        } catch (e: Exception) {
            crashed(e)
        }
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit {
        if (checkStatus()) try {
            workspaceService.didChangeWatchedFiles(params)
        } catch (e: Exception) {
            crashed(e)
        }
    }

    override fun symbol(params: WorkspaceSymbolParams): CompletableFuture<List<SymbolInformation>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.workspaceSymbolProvider) workspaceService.symbol(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any>? {
        return if (checkStatus()) try {
            if (serverCapabilities.executeCommandProvider != null) workspaceService.executeCommand(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    //TextDocument
    override fun didOpen(params: DidOpenTextDocumentParams): Unit {
        if (checkStatus()) try {
            if (textDocumentOptions == null || textDocumentOptions.openClose) textDocumentService.didOpen(params)
        } catch (e: Exception) {
            crashed(e)
        }
    }

    override fun didChange(params: DidChangeTextDocumentParams): Unit {
        if (checkStatus()) try {
            if (textDocumentOptions == null || textDocumentOptions.change != null) textDocumentService.didChange(params)
        } catch (e: Exception) {
            crashed(e)
        }
    }

    override fun willSave(params: WillSaveTextDocumentParams): Unit {
        if (checkStatus()) try {
            if (textDocumentOptions == null || textDocumentOptions.willSave) textDocumentService.willSave(params)
        } catch (e: Exception) {
            crashed(e)
        }
    }

    override fun willSaveWaitUntil(params: WillSaveTextDocumentParams): CompletableFuture<List<TextEdit>>? {
        return if (checkStatus()) try {
            if (textDocumentOptions == null || textDocumentOptions.willSaveWaitUntil) textDocumentService.willSaveWaitUntil(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun didSave(params: DidSaveTextDocumentParams): Unit {
        if (checkStatus()) try {
            if (textDocumentOptions == null || textDocumentOptions.save != null) textDocumentService.didSave(params)
        } catch (e: Exception) {
            crashed(e)
        }
    }

    override fun didClose(params: DidCloseTextDocumentParams): Unit {
        if (checkStatus()) try {
            if (textDocumentOptions == null || textDocumentOptions.openClose) textDocumentService.didClose(params)
        } catch (e: Exception) {
            crashed(e)
        }
    }

    override fun completion(params: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.completionProvider != null) textDocumentService.completion(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun resolveCompletionItem(unresolved: CompletionItem): CompletableFuture<CompletionItem>? {
        return if (checkStatus()) try {
            if (serverCapabilities.completionProvider != null && serverCapabilities.completionProvider.resolveProvider) textDocumentService.resolveCompletionItem(
                unresolved
            ) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun hover(params: TextDocumentPositionParams): CompletableFuture<Hover>? {
        return if (checkStatus()) try {
            if (serverCapabilities.hoverProvider) textDocumentService.hover(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun signatureHelp(params: TextDocumentPositionParams): CompletableFuture<SignatureHelp>? {
        return if (checkStatus()) try {
            if (serverCapabilities.signatureHelpProvider != null) textDocumentService.signatureHelp(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun references(params: ReferenceParams): CompletableFuture<List<Location>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.referencesProvider) textDocumentService.references(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun documentHighlight(params: TextDocumentPositionParams): CompletableFuture<List<DocumentHighlight>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.documentHighlightProvider) textDocumentService.documentHighlight(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.documentSymbolProvider) textDocumentService.documentSymbol(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun formatting(params: DocumentFormattingParams): CompletableFuture<List<TextEdit>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.documentFormattingProvider) textDocumentService.formatting(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture<List<TextEdit>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.documentRangeFormattingProvider) textDocumentService.rangeFormatting(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun onTypeFormatting(params: DocumentOnTypeFormattingParams): CompletableFuture<List<TextEdit>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.documentOnTypeFormattingProvider != null) textDocumentService.onTypeFormatting(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun definition(params: TextDocumentPositionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.definitionProvider) textDocumentService.definition(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun codeAction(params: CodeActionParams): CompletableFuture<List<Either<Command, CodeAction>>>? {
        return if (checkStatus()) try {
            if (checkProvider(serverCapabilities.codeActionProvider)) textDocumentService.codeAction(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    private fun <T : Any> checkProvider(provider: Either<Boolean?, T?>?): Boolean =
        provider?.get() != null

    override fun codeLens(params: CodeLensParams): CompletableFuture<List<CodeLens>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.codeLensProvider != null) textDocumentService.codeLens(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun resolveCodeLens(unresolved: CodeLens): CompletableFuture<CodeLens>? {
        return if (checkStatus()) try {
            if (serverCapabilities.codeLensProvider != null && serverCapabilities.codeLensProvider.isResolveProvider) textDocumentService.resolveCodeLens(
                unresolved
            ) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun documentLink(params: DocumentLinkParams): CompletableFuture<List<DocumentLink>>? {
        return if (checkStatus()) try {
            if (serverCapabilities.documentLinkProvider != null) textDocumentService.documentLink(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    private fun checkStatus(): Boolean = wrapper.status == ServerStatus.STARTED

    private fun crashed(e: Exception): Unit {
        logger.warn(e)
        wrapper.crashed(e)
    }

    override fun documentLinkResolve(unresolved: DocumentLink): CompletableFuture<DocumentLink>? {
        return if (checkStatus()) try {
            if (serverCapabilities.documentLinkProvider != null && serverCapabilities.documentLinkProvider.resolveProvider)
                textDocumentService.documentLinkResolve(unresolved) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun rename(params: RenameParams): CompletableFuture<WorkspaceEdit>? {
        return if (checkStatus()) try {
            if (checkProvider(serverCapabilities.renameProvider)) textDocumentService.rename(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun prepareRename(params: TextDocumentPositionParams): CompletableFuture<Either<Range, PrepareRenameResult>>? {
        return if (checkStatus()) try {
            if (checkProvider(serverCapabilities.renameProvider)) textDocumentService.prepareRename(params) else null
        } catch (e: Exception) {
            crashed(e)
            null
        } else null
    }

    override fun implementation(params: TextDocumentPositionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        throw NotImplementedError()
    }

    override fun typeDefinition(params: TextDocumentPositionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        throw NotImplementedError()
    }

    override fun documentColor(params: DocumentColorParams): CompletableFuture<List<ColorInformation>> {
        throw NotImplementedError()
    }

    override fun colorPresentation(params: ColorPresentationParams): CompletableFuture<List<ColorPresentation>> {
        throw NotImplementedError()
    }

    override fun foldingRange(params: FoldingRangeRequestParams): CompletableFuture<List<FoldingRange>> {
        throw NotImplementedError()
    }

    override fun getWorkspaceService(): WorkspaceService {
        return workspaceService
    }

    override fun getTextDocumentService(): TextDocumentService {
        return textDocumentService
    }
}