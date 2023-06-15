package com.github.gtache.lsp.settings.project

import com.github.gtache.lsp.languageserver.definition.Definition
import com.github.gtache.lsp.settings.project.gui.ServersSettingsGUI
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Class used to manage the settings related to the LSP
 */
class ServersConfigurable(private val project: Project) : Configurable {

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
        val gui = ServersSettingsGUI(project)
        serversGUI = gui
        setGUIFields(project.service<LSPPersistentProjectSettings>().projectState.extensionToServerDefinition)
        return gui.getRootPanel()
    }

    private fun setGUIFields(map: Map<String, Definition>) {
        if (map.isNotEmpty()) {
            serversGUI?.clear()
        }
        for (definition in map.values) {
            serversGUI?.addServerDefinition(definition)
        }
    }

    override fun isModified(): Boolean {
        return serversGUI?.isModified() ?: false
    }

    override fun apply() {
        serversGUI?.apply()
    }

    override fun reset() {
        serversGUI?.reset()
    }

    override fun disposeUIResources() {
        serversGUI = null
    }

    companion object {
        private var serversGUI: ServersSettingsGUI? = null
    }
}