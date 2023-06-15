package com.github.gtache.lsp.languageserver.configuration.provider

import com.github.gtache.lsp.languageserver.configuration.Configuration
import com.github.gtache.lsp.languageserver.configuration.parser.ConfigType
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Provider of Configurations
 */
interface ConfigurationProvider {


    /**
     * Returns the configuration for the given [file] and optional [charset]
     */
    fun getConfiguration(file: File, charset: Charset = StandardCharsets.UTF_8): Configuration?

    /**
     * Returns the configuration for the given [document] and [type]
     */
    fun getConfiguration(document: String, type: ConfigType): Configuration?

    /**
     * Returns the configuration for the given [files] and optional [charset]
     */
    fun getConfiguration(files: List<File>, charset: Charset = StandardCharsets.UTF_8): Configuration

    /**
     * Returns an invalid configuration
     */
    fun getInvalidConfiguration(): Configuration

    /**
     * Returns an empty configuration
     */
    fun getEmptyConfiguration(): Configuration

    /**
     * Combines two configurations [firstConfig] and [secondConfig]
     */
    fun combineConfigurations(firstConfig: Map<String, Map<String, Any?>>, secondConfig: Map<String, Map<String, Any?>>): Map<String, Map<String, Any?>>
}