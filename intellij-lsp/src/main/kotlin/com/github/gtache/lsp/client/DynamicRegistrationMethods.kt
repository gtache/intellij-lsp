package com.github.gtache.lsp.client

/**
 * Enum for methods which may support DynamicRegistration
 */
enum class DynamicRegistrationMethods(private val methodName: String) {
    DID_CHANGE_CONFIGURATION("workspace/didChangeConfiguration"),
    DID_CHANGE_WATCHED_FILES("workspace/didChangeWatchedFiles"),
    SYMBOL("workspace/symbol"),
    EXECUTE_COMMAND("workspace/executeCommand"),
    SYNCHRONIZATION("textDocument/synchronization"),
    COMPLETION("textDocument/completion"),
    HOVER("textDocument/hover"),
    SIGNATURE_HELP("textDocument/signatureHelp"),
    REFERENCES("textDocument/references"),
    DOCUMENT_HIGHLIGHT("textDocument/documentHighlight"),
    DOCUMENT_SYMBOL("textDocument/documentSymbol"),
    FORMATTING("textDocument/formatting"),
    RANGE_FORMATTING("textDocument/rangeFormatting"),
    ONTYPE_FORMATTING("textDocument/onTypeFormatting"),
    DEFINITION("textDocument/definition"),
    CODE_ACTION("textDocument/codeAction"),
    CODE_LENS("textDocument/codeLens"),
    DOCUMENT_LINK("textDocument/documentLink"),
    RENAME("textDocument/rename");

    companion object {
        /**
         * Returns the method for the given [methodName]
         */
        fun forName(methodName: String): DynamicRegistrationMethods? {
            return values().firstOrNull { n -> n.methodName == methodName }
        }
    }

    /**
     * Returns the name of the method
     */
    fun getName(): String {
        return methodName
    }
}