package com.github.gtache.lsp.client.languageserver

import org.eclipse.lsp4j.*

object DummyServerOptions {
    val SYNC_KIND = TextDocumentSyncKind.None
    val RENAME = RenameOptions()
    val COMPLETION = CompletionOptions()
    val CODELENS = CodeLensOptions()
    val SIGNATURE_HELP = SignatureHelpOptions()
    val DOCUMENT_ON_TYPE_FORMATTING = DocumentOnTypeFormattingOptions()
    val SEMANTIC_HIGHLIGHTING = SemanticHighlightingServerCapabilities()
    val DOCUMENT_LINK = DocumentLinkOptions()
    val EXECUTE_COMMAND = ExecuteCommandOptions()
}