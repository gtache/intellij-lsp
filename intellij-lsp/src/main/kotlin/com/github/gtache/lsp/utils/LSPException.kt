package com.github.gtache.lsp.utils

/**
 * Represents an exception relative to an LSP Server
 * @constructor Creates the exception for the given message
 */
abstract class LSPException(m: String) : RuntimeException(m)