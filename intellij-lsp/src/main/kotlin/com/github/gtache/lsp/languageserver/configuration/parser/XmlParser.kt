package com.github.gtache.lsp.languageserver.configuration.parser

import com.github.gtache.lsp.languageserver.configuration.Configuration
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A configuration parser for XML
 */
class XmlParser : ConfigurationParser {
    override fun parse(document: String): Configuration {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(document)
        val root = doc.documentElement
        throw UnsupportedOperationException()
    }
}