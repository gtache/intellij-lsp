package com.github.gtache.lsp.languageserver.configuration.parser

import com.github.gtache.lsp.languageserver.configuration.Configuration

/**
 * A configuration parser for flat (txt, ini) files
 */
class FlatParser : ConfigurationParser {
    override fun parse(document: String): Configuration {
        throw UnsupportedOperationException()
    }
}