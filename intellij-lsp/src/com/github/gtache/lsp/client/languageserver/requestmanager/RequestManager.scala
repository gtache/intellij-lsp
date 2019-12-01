package com.github.gtache.lsp.client.languageserver.requestmanager

import java.util
import java.util.concurrent.CompletableFuture

import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.jsonrpc.messages.CancelParams
import org.eclipse.lsp4j.services.{LanguageClient, LanguageServer, TextDocumentService, WorkspaceService}

/**
  * Handles requests between server and client
  */
trait RequestManager extends LanguageServer with TextDocumentService with WorkspaceService with LanguageClient {

  //Client
  override def showMessage(messageParams: MessageParams): Unit

  override def showMessageRequest(showMessageRequestParams: ShowMessageRequestParams): CompletableFuture[MessageActionItem]

  override def logMessage(messageParams: MessageParams): Unit

  override def telemetryEvent(o: Any): Unit

  override def registerCapability(params: RegistrationParams): CompletableFuture[Void]

  override def unregisterCapability(params: UnregistrationParams): CompletableFuture[Void]

  override def applyEdit(params: ApplyWorkspaceEditParams): CompletableFuture[ApplyWorkspaceEditResponse]

  override def publishDiagnostics(publishDiagnosticsParams: PublishDiagnosticsParams): Unit

  override def configuration(configurationParams: ConfigurationParams): CompletableFuture[util.List[AnyRef]]

  //Server
  //General
  override def initialize(params: InitializeParams): CompletableFuture[InitializeResult]

  override def initialized(params: InitializedParams): Unit

  override def shutdown: CompletableFuture[AnyRef]

  override def exit(): Unit

  def cancelRequest(params: CancelParams): Unit

  //Workspace
  override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit

  override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit

  override def symbol(params: WorkspaceSymbolParams): CompletableFuture[java.util.List[_ <: SymbolInformation]]

  override def executeCommand(params: ExecuteCommandParams): CompletableFuture[AnyRef]

  override def didChangeWorkspaceFolders(params: DidChangeWorkspaceFoldersParams): Unit

  //Document
  override def didOpen(params: DidOpenTextDocumentParams): Unit

  override def didChange(params: DidChangeTextDocumentParams): Unit

  override def willSave(params: WillSaveTextDocumentParams): Unit

  override def willSaveWaitUntil(params: WillSaveTextDocumentParams): CompletableFuture[java.util.List[TextEdit]]

  override def didSave(params: DidSaveTextDocumentParams): Unit

  override def didClose(params: DidCloseTextDocumentParams): Unit

  override def completion(params: CompletionParams): CompletableFuture[jsonrpc.messages.Either[java.util.List[CompletionItem], CompletionList]]

  override def resolveCompletionItem(unresolved: CompletionItem): CompletableFuture[CompletionItem]

  override def hover(params: TextDocumentPositionParams): CompletableFuture[Hover]

  override def signatureHelp(params: TextDocumentPositionParams): CompletableFuture[SignatureHelp]

  override def references(params: ReferenceParams): CompletableFuture[java.util.List[_ <: Location]]

  override def documentHighlight(params: TextDocumentPositionParams): CompletableFuture[java.util.List[_ <: DocumentHighlight]]

  override def documentSymbol(params: DocumentSymbolParams): CompletableFuture[java.util.List[jsonrpc.messages.Either[SymbolInformation, DocumentSymbol]]]

  override def formatting(params: DocumentFormattingParams): CompletableFuture[java.util.List[_ <: TextEdit]]

  override def rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture[java.util.List[_ <: TextEdit]]

  override def onTypeFormatting(params: DocumentOnTypeFormattingParams): CompletableFuture[java.util.List[_ <: TextEdit]]

  override def definition(params: TextDocumentPositionParams): CompletableFuture[jsonrpc.messages.Either[java.util.List[_ <: Location], java.util.List[_ <: LocationLink]]]

  override def codeAction(params: CodeActionParams): CompletableFuture[java.util.List[jsonrpc.messages.Either[Command, CodeAction]]]

  override def codeLens(params: CodeLensParams): CompletableFuture[java.util.List[_ <: CodeLens]]

  override def resolveCodeLens(unresolved: CodeLens): CompletableFuture[CodeLens]

  override def documentLink(params: DocumentLinkParams): CompletableFuture[java.util.List[DocumentLink]]

  override def documentLinkResolve(unresolved: DocumentLink): CompletableFuture[DocumentLink]

  override def rename(params: RenameParams): CompletableFuture[WorkspaceEdit]

  override def implementation(params: TextDocumentPositionParams): CompletableFuture[messages.Either[java.util.List[_ <: Location], java.util.List[_ <: LocationLink]]]

  override def typeDefinition(params: TextDocumentPositionParams): CompletableFuture[messages.Either[java.util.List[_ <: Location], java.util.List[_ <: LocationLink]]]

  override def documentColor(params: DocumentColorParams): CompletableFuture[java.util.List[ColorInformation]]

  override def colorPresentation(params: ColorPresentationParams): CompletableFuture[java.util.List[ColorPresentation]]

  override def foldingRange(params: FoldingRangeRequestParams): CompletableFuture[java.util.List[FoldingRange]]

  override def semanticHighlighting(params: SemanticHighlightingParams): Unit

  override def declaration(params: TextDocumentPositionParams): CompletableFuture[messages.Either[util.List[_ <: Location], util.List[_ <: LocationLink]]]

  override def prepareRename(params: TextDocumentPositionParams): CompletableFuture[messages.Either[Range, PrepareRenameResult]]

  override def callHierarchy(params: CallHierarchyParams): CompletableFuture[util.List[CallHierarchyCall]]

  override def typeHierarchy(params: TypeHierarchyParams): CompletableFuture[TypeHierarchyItem]

  override def resolveTypeHierarchy(params: ResolveTypeHierarchyItemParams): CompletableFuture[TypeHierarchyItem]


  //Unused
  override def getTextDocumentService: TextDocumentService = throw new UnsupportedOperationException

  override def getWorkspaceService: WorkspaceService = throw new UnsupportedOperationException

}
