package com.github.gtache.lsp.contributors.icon

import com.github.gtache.lsp.client.languageserver.status.ServerStatus
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.intellij.openapi.extensions.ExtensionPointName
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.SymbolKind
import javax.swing.Icon


interface LSPIconProvider {

    companion object {
        val EP_NAME: ExtensionPointName<LSPIconProvider> = ExtensionPointName.create("com.github.gtache.lsp.lspIconProvider")

        fun getDefaultCompletionIcon(kind: CompletionItemKind): Icon? = LSPDefaultIconProvider.getCompletionIcon(kind)

        fun getDefaultStatusIcons(): Map<ServerStatus, Icon> = LSPDefaultIconProvider.statusIcons

        fun getDefaultSymbolIcon(kind: SymbolKind): Icon? = LSPDefaultIconProvider.getSymbolIcon(kind)
    }

    val statusIcons: Map<ServerStatus, Icon>
        get() = LSPDefaultIconProvider.statusIcons

    fun getCompletionIcon(kind: CompletionItemKind): Icon? = LSPDefaultIconProvider.getCompletionIcon(kind)

    fun getSymbolIcon(kind: SymbolKind): Icon? = LSPDefaultIconProvider.getSymbolIcon(kind)

    fun isSpecificFor(serverDefinition: LanguageServerDefinition): Boolean
}