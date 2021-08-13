package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.client.connection.StreamConnectionProvider

abstract class BaseServerDefinition : LanguageServerDefinition {
    private val _mappedExtensions: MutableSet<String> by lazy {
        splitExtension(ext).toMutableSet()
    }
    override val mappedExtensions: MutableSet<String>
        get() = HashSet(_mappedExtensions)

    override val streamConnectionProviders: MutableMap<String, StreamConnectionProvider> = HashMap()
}