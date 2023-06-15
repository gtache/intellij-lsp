package com.github.gtache.lsp.languageserver.configuration.parser

import com.github.gtache.lsp.languageserver.configuration.Configuration
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Represents a parser of configurations
 */
interface ConfigurationParser {
    /**
     * Parses the given file
     */
    fun parse(file: File, charset: Charset = StandardCharsets.UTF_8): Configuration {
        return parse(file.readText(charset))
    }

    /**
     * Parses the given [document] configuration
     */
    fun parse(document: String): Configuration

}