package com.github.gtache.lsp.contributors.gotoo

import com.github.gtache.lsp.PluginMain
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * This interface is the base implementation of a GotoContributor
 */
interface LSPGotoContributor : ChooseByNameContributor {
    companion object {
        protected val logger: Logger = Logger.getInstance(LSPGotoContributor::class.java)
    }

    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        return PluginMain.workspaceSymbols("", "", project, includeNonProjectItems).mapNotNull { f -> f.name }.toTypedArray()
    }
}