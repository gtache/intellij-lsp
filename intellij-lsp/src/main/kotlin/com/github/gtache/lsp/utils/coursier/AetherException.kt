package com.github.gtache.lsp.utils.coursier

import com.github.gtache.lsp.utils.LSPException

data class AetherException(val msg: String) : LSPException(msg)

