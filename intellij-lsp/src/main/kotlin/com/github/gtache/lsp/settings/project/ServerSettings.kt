package com.github.gtache.lsp.settings.project

import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.client.languageserver.serverdefinition.UserConfigurableServerDefinition
import com.github.gtache.lsp.services.project.LSPProjectService
import com.github.gtache.lsp.settings.project.gui.ServersGUI
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Class used to manage the settings related to the LSP
 */
class ServersSettings(private val project: Project) : Configurable {

    /**
     * Returns the display name of the settings group
     */
    override fun getDisplayName(): @Nls String {
        return "Language Server Protocol"
    }

    /**
     * Returns the help topic of the settings group
     */
    override fun getHelpTopic(): String {
        return "com.github.gtache.lsp.settings.project.ServersSettings"
    }

    /**
     * Creates the settings component
     */
    override fun createComponent(): JComponent {
        lspGUI = ServersGUI(project)
        setGUIFields(project.service<LSPProjectService>().extensionsToServerDefinitions)
        return lspGUI!!.getRootPanel()
    }

    private fun setGUIFields(map: Map<String, LanguageServerDefinition>) {
        val filtered = map.filterValues { it is UserConfigurableServerDefinition }.toMap()
        if (filtered.isNotEmpty()) {
            lspGUI?.clear()
        }
        for (definition in filtered.values) {
            lspGUI?.addServerDefinition(definition as UserConfigurableServerDefinition)
        }
    }

    override fun isModified(): Boolean {
        return lspGUI?.isModified() ?: false
    }

    override fun apply() {
        lspGUI?.apply()
    }

    override fun reset() {
        lspGUI?.reset()
    }

    override fun disposeUIResources() {
        lspGUI = null
    }

    companion object {
        private var lspGUI: ServersGUI? = null
    }
}