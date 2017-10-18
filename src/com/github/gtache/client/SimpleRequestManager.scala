package com.github.gtache.client

import java.util.concurrent.CompletableFuture

import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.messages.CancelParams
import org.eclipse.lsp4j.services.{LanguageClient, LanguageServer, TextDocumentService, WorkspaceService}

/**
  * Basic implementation of a RequestManager which just passes requests from client to server and vice-versa
  */
class SimpleRequestManager(server: LanguageServer, client: LanguageClient) extends RequestManager {

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

  override def symbol(params: WorkspaceSymbolParams): CompletableFuture[java.util.List[_ <: SymbolInformation]] = workspaceService.symbol(params)

  override def executeCommand(params: ExecuteCommandParams): CompletableFuture[AnyRef] = workspaceService.executeCommand(params)

  //TextDocument
  override def didOpen(params: DidOpenTextDocumentParams): Unit = textDocumentService.didOpen(params)

  override def didChange(params: DidChangeTextDocumentParams): Unit = textDocumentService.didChange(params)

  override def willSave(params: WillSaveTextDocumentParams): Unit = textDocumentService.willSave(params)

  override def willSaveWaitUntil(params: WillSaveTextDocumentParams): CompletableFuture[java.util.List[TextEdit]] = textDocumentService.willSaveWaitUntil(params)

  override def didSave(params: DidSaveTextDocumentParams): Unit = textDocumentService.didSave(params)

  override def didClose(params: DidCloseTextDocumentParams): Unit = textDocumentService.didClose(params)

  override def completion(params: TextDocumentPositionParams): CompletableFuture[jsonrpc.messages.Either[java.util.List[CompletionItem], CompletionList]] = textDocumentService.completion(params)

  override def completionItemResolve(unresolved: CompletionItem): CompletableFuture[CompletionItem] = textDocumentService.resolveCompletionItem(unresolved)

  override def hover(params: TextDocumentPositionParams): CompletableFuture[Hover] = textDocumentService.hover(params)

  override def signatureHelp(params: TextDocumentPositionParams): CompletableFuture[SignatureHelp] = textDocumentService.signatureHelp(params)

  override def references(params: ReferenceParams): CompletableFuture[java.util.List[_ <: Location]] = textDocumentService.references(params)

  override def documentHighlight(params: TextDocumentPositionParams): CompletableFuture[java.util.List[_ <: DocumentHighlight]] = textDocumentService.documentHighlight(params)

  override def documentSymbol(params: DocumentSymbolParams): CompletableFuture[java.util.List[_ <: SymbolInformation]] = textDocumentService.documentSymbol(params)

  override def formatting(params: DocumentFormattingParams): CompletableFuture[java.util.List[_ <: TextEdit]] = textDocumentService.formatting(params)

  override def rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture[java.util.List[_ <: TextEdit]] = textDocumentService.rangeFormatting(params)

  override def onTypeFormatting(params: DocumentOnTypeFormattingParams): CompletableFuture[java.util.List[_ <: TextEdit]] = textDocumentService.onTypeFormatting(params)

  override def definition(params: TextDocumentPositionParams): CompletableFuture[java.util.List[_ <: Location]] = textDocumentService.definition(params)

  override def codeAction(params: CodeActionParams): CompletableFuture[java.util.List[_ <: Command]] = textDocumentService.codeAction(params)

  override def codeLens(params: CodeLensParams): CompletableFuture[java.util.List[_ <: CodeLens]] = textDocumentService.codeLens(params)

  override def resolveCodeLens(unresolved: CodeLens): CompletableFuture[CodeLens] = textDocumentService.resolveCodeLens(unresolved)

  override def documentLink(params: DocumentLinkParams): CompletableFuture[java.util.List[DocumentLink]] = textDocumentService.documentLink(params)

  override def documentLinkResolve(unresolved: DocumentLink): CompletableFuture[DocumentLink] = textDocumentService.documentLinkResolve(unresolved)

  override def rename(params: RenameParams): CompletableFuture[WorkspaceEdit] = textDocumentService.rename(params)
}
