package com.github.gtache.lsp.languageserver.definition

import com.github.gtache.lsp.utils.LSPException

/**
 * Exception thrown when the server definition is not correctly specified
 */
data class BadDefinitionException(val m: String) : LSPException(m)