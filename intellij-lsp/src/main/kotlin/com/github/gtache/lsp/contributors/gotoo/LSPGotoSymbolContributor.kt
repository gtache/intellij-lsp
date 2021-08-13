package com.github.gtache.lsp.contributors.gotoo

import com.github.gtache.lsp.PluginMain
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project

/**
 * The GotoSymbol contributor for LSP
 */
class LSPGotoSymbolContributor : LSPGotoContributor {

    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array<NavigationItem> {
        return PluginMain.workspaceSymbols(name, pattern, project, includeNonProjectItems)
    }
}