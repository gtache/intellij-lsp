package com.github.gtache.lsp.client.languageserver

import org.eclipse.lsp4j.*

/**
 * Class containing the options of the language server
 *
 * @param syncKind                        The type of synchronization
 * @param completionOptions               The completion options
 * @param signatureHelpOptions            The signatureHelp options
 * @param codeLensOptions                 The codeLens options
 * @param documentOnTypeFormattingOptions The onTypeFormatting options
 * @param documentLinkOptions             The link options
 * @param executeCommandOptions           The execute options
 * @param semanticHighlightingOptions     The semantic highlight options
 * @param renameOptions                   The rename options
 */
data class ServerOptions(
    val syncKind: TextDocumentSyncKind,
    val completionOptions: CompletionOptions,
    val signatureHelpOptions: SignatureHelpOptions,
    val codeLensOptions: CodeLensOptions,
    val documentOnTypeFormattingOptions: DocumentOnTypeFormattingOptions,
    val documentLinkOptions: DocumentLinkOptions,
    val executeCommandOptions: ExecuteCommandOptions,
    val semanticHighlightingOptions: SemanticHighlightingServerCapabilities,
    val renameOptions: RenameOptions
)