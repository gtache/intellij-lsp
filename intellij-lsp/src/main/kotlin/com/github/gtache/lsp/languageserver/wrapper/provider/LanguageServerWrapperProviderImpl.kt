package com.github.gtache.lsp.languageserver.wrapper.provider

import com.github.gtache.lsp.languageserver.definition.Definition
import com.github.gtache.lsp.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.languageserver.wrapper.LanguageServerWrapperImpl
import com.intellij.openapi.project.Project

/**
 * Implementation of LanguageServerWrapperProvider
 */
class LanguageServerWrapperProviderImpl(private val project: Project) : LanguageServerWrapperProvider {

    private val instantiated: MutableMap<Definition, LanguageServerWrapper> = HashMap()
    override fun getWrapper(definition: Definition): LanguageServerWrapper {
        val existing = instantiated[definition]
        return if (existing != null) {
            existing
        } else {
            val wrapper = LanguageServerWrapperImpl(definition, project)
            instantiated[definition] = wrapper
            wrapper
        }
    }
}