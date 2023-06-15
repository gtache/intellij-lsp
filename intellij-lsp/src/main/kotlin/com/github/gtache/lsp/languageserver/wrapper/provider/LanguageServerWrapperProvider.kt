package com.github.gtache.lsp.languageserver.wrapper.provider

import com.github.gtache.lsp.languageserver.definition.Definition
import com.github.gtache.lsp.languageserver.wrapper.LanguageServerWrapper

/**
 * Provides Language Server Wrappers
 */
interface LanguageServerWrapperProvider {
    /**
     * Returns the wrapper for the given language server [definition]
     */
    fun getWrapper(definition: Definition): LanguageServerWrapper
}