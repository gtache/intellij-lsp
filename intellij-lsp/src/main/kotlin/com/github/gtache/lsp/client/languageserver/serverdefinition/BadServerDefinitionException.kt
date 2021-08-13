package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.utils.LSPException

data class BadServerDefinitionException(val m: String) : LSPException(m)