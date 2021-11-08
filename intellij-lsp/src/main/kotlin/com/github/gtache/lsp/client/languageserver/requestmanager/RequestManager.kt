package com.github.gtache.lsp.client.languageserver.requestmanager


import org.eclipse.lsp4j.jsonrpc.messages.CancelParams
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService

/**
 * Handles requests between server and client
 */
interface RequestManager : LanguageServer, TextDocumentService, WorkspaceService, LanguageClient {

    /**
     * Cancel the request specified by the given [params]
     */
    fun cancelRequest(params: CancelParams): Unit

}