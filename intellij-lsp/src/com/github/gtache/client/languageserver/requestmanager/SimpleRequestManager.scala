package com.github.gtache.client.languageserver.requestmanager

import java.util.concurrent.CompletableFuture

import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.messages.CancelParams
import org.eclipse.lsp4j.services.{LanguageClient, LanguageServer, TextDocumentService, WorkspaceService}

/**
  * Basic implementation of a RequestManager which just passes requests from client to server and vice-versa
  */
class SimpleRequestManager(server: LanguageServer, client: LanguageClient, serverCapabilities: ServerCapabilities) extends RequestManager {

  private val textDocumentOptions = if (serverCapabilities.getTextDocumentSync.isRight) serverCapabilities.getTextDocumentSync.getRight else null
  private val workspaceService: WorkspaceService = server.getWorkspaceService
  private val textDocumentService: TextDocumentService = server.getTextDocumentService

  //Client
  override def showMessage(messageParams: MessageParams): Unit = client.showMessage(messageParams)

  override def showMessageRequest(showMessageRequestParams: ShowMessageRequestParams): CompletableFuture[MessageActionItem] = client.showMessageRequest(showMessageRequestParams)

  override def logMessage(messageParams: MessageParams): Unit = client.logMessage(messageParams)

  override def telemetryEvent(o: Any): Unit = client.telemetryEvent(o)

  override def registerCapability(params: RegistrationParams): CompletableFuture[Void] = client.registerCapability(params)

  override def unregisterCapability(params: UnregistrationParams): CompletableFuture[Void] = client.unregisterCapability(params)

  override def applyEdit(params: ApplyWorkspaceEditParams): CompletableFuture[ApplyWorkspaceEditResponse] = client.applyEdit(params)

  override def publishDiagnostics(publishDiagnosticsParams: PublishDiagnosticsParams): Unit = client.publishDiagnostics(publishDiagnosticsParams)

  //General
  override def initialize(params: InitializeParams): CompletableFuture[InitializeResult] = server.initialize(params)

  override def initialized(params: InitializedParams): Unit = server.initialized(params)

  override def shutdown: CompletableFuture[AnyRef] = server.shutdown()

  override def exit(): Unit = server.exit()

  override def cancelRequest(params: CancelParams): Unit = {
  }


  //Workspace
  override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = workspaceService.didChangeConfiguration(params)

  override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit = workspaceService.didChangeWatchedFiles(params)

  override def symbol(params: WorkspaceSymbolParams): CompletableFuture[java.util.List[_ <: SymbolInformation]] = if (serverCapabilities.getWorkspaceSymbolProvider) workspaceService.symbol(params) else null

  override def executeCommand(params: ExecuteCommandParams): CompletableFuture[AnyRef] = if (serverCapabilities.getExecuteCommandProvider != null) workspaceService.executeCommand(params) else null

  //TextDocument
  override def didOpen(params: DidOpenTextDocumentParams): Unit = if (textDocumentOptions != null && textDocumentOptions.getOpenClose) textDocumentService.didOpen(params)

  override def didChange(params: DidChangeTextDocumentParams): Unit = if (textDocumentOptions != null && textDocumentOptions.getChange != null) textDocumentService.didChange(params)

  override def willSave(params: WillSaveTextDocumentParams): Unit = if (textDocumentOptions != null && textDocumentOptions.getWillSave) textDocumentService.willSave(params)

  override def willSaveWaitUntil(params: WillSaveTextDocumentParams): CompletableFuture[java.util.List[TextEdit]] = if (textDocumentOptions != null && textDocumentOptions.getWillSaveWaitUntil) textDocumentService.willSaveWaitUntil(params) else null

  override def didSave(params: DidSaveTextDocumentParams): Unit = if (textDocumentOptions != null && textDocumentOptions.getSave != null) textDocumentService.didSave(params)

  override def didClose(params: DidCloseTextDocumentParams): Unit = if (textDocumentOptions != null && textDocumentOptions.getOpenClose) textDocumentService.didClose(params)

  override def completion(params: TextDocumentPositionParams): CompletableFuture[jsonrpc.messages.Either[java.util.List[CompletionItem], CompletionList]] = if (serverCapabilities.getCompletionProvider != null) textDocumentService.completion(params) else null

  override def completionItemResolve(unresolved: CompletionItem): CompletableFuture[CompletionItem] = if (serverCapabilities.getCompletionProvider != null && serverCapabilities.getCompletionProvider.getResolveProvider) textDocumentService.resolveCompletionItem(unresolved) else null

  override def hover(params: TextDocumentPositionParams): CompletableFuture[Hover] = if (serverCapabilities.getHoverProvider) textDocumentService.hover(params) else null

  override def signatureHelp(params: TextDocumentPositionParams): CompletableFuture[SignatureHelp] = if (serverCapabilities.getSignatureHelpProvider != null) textDocumentService.signatureHelp(params) else null

  override def references(params: ReferenceParams): CompletableFuture[java.util.List[_ <: Location]] = if (serverCapabilities.getReferencesProvider) textDocumentService.references(params) else null

  override def documentHighlight(params: TextDocumentPositionParams): CompletableFuture[java.util.List[_ <: DocumentHighlight]] = if (serverCapabilities.getDocumentHighlightProvider) textDocumentService.documentHighlight(params) else null

  override def documentSymbol(params: DocumentSymbolParams): CompletableFuture[java.util.List[_ <: SymbolInformation]] = if (serverCapabilities.getDocumentSymbolProvider) textDocumentService.documentSymbol(params) else null

  override def formatting(params: DocumentFormattingParams): CompletableFuture[java.util.List[_ <: TextEdit]] = if (serverCapabilities.getDocumentFormattingProvider) textDocumentService.formatting(params) else null

  override def rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture[java.util.List[_ <: TextEdit]] = if (serverCapabilities.getDocumentRangeFormattingProvider) textDocumentService.rangeFormatting(params) else null

  override def onTypeFormatting(params: DocumentOnTypeFormattingParams): CompletableFuture[java.util.List[_ <: TextEdit]] = if (serverCapabilities.getDocumentOnTypeFormattingProvider != null) textDocumentService.onTypeFormatting(params) else null

  override def definition(params: TextDocumentPositionParams): CompletableFuture[java.util.List[_ <: Location]] = if (serverCapabilities.getDefinitionProvider) textDocumentService.definition(params) else null

  override def codeAction(params: CodeActionParams): CompletableFuture[java.util.List[_ <: Command]] = if (serverCapabilities.getCodeActionProvider) textDocumentService.codeAction(params) else null

  override def codeLens(params: CodeLensParams): CompletableFuture[java.util.List[_ <: CodeLens]] = if (serverCapabilities.getCodeLensProvider != null) textDocumentService.codeLens(params) else null

  override def resolveCodeLens(unresolved: CodeLens): CompletableFuture[CodeLens] = if (serverCapabilities.getCodeLensProvider != null && serverCapabilities.getCodeLensProvider.isResolveProvider) textDocumentService.resolveCodeLens(unresolved) else null

  override def documentLink(params: DocumentLinkParams): CompletableFuture[java.util.List[DocumentLink]] = if (serverCapabilities.getDocumentLinkProvider != null) textDocumentService.documentLink(params) else null

  override def documentLinkResolve(unresolved: DocumentLink): CompletableFuture[DocumentLink] = if (serverCapabilities.getDocumentLinkProvider != null && serverCapabilities.getDocumentLinkProvider.getResolveProvider) textDocumentService.documentLinkResolve(unresolved) else null

  override def rename(params: RenameParams): CompletableFuture[WorkspaceEdit] = if (serverCapabilities.getRenameProvider) textDocumentService.rename(params) else null
}
