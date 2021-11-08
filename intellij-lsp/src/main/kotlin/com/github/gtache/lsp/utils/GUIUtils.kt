package com.github.gtache.lsp.utils

import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.contributors.icon.LSPIconProvider
import com.github.gtache.lsp.head

/**
 * Various utility methods related to the interface
 */
object GUIUtils {

    /**
     * Returns a suitable LSPIconProvider given a [serverDefinition], or the default one
     */
    fun getIconProviderFor(serverDefinition: LanguageServerDefinition): LSPIconProvider {
        return try {
            val providers = LSPIconProvider.EP_NAME.extensions.filter { provider -> provider.isSpecificFor(serverDefinition) }
            if (providers.isNotEmpty()) providers.head else LSPIconProvider.DefaultIconProvider
        } catch (e: IllegalArgumentException) {
            LSPIconProvider.DefaultIconProvider
        }
    }
}