package com.github.gtache.lsp.utils.aether

import com.github.gtache.lsp.utils.LSPException

/**
 * Exception related to Aether
 * @constructor [msg] The exception message
 */
data class AetherException(val msg: String) : LSPException(msg)

