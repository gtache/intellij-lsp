package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.utils.LSPException

/**
 * Exception thrown when the server definition is not correctly specified
 */
data class BadServerDefinitionException(val m: String) : LSPException(m)