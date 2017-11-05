package com.github.gtache.client.languageserver

import org.eclipse.lsp4j._

case class ServerOptions(syncKind: TextDocumentSyncKind, completionOptions: CompletionOptions, signatureHelpOptions: SignatureHelpOptions, codeLensOptions: CodeLensOptions, documentOnTypeFormattingOptions: DocumentOnTypeFormattingOptions, documentLinkOptions: DocumentLinkOptions, executeCommandOptions: ExecuteCommandOptions) {

}
