package com.github.gtache.lsp.settings

import com.github.gtache.lsp.PluginMain
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.client.languageserver.serverdefinition.UserConfigurableServerDefinition
import com.github.gtache.lsp.settings.gui.ServersGUI
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Class used to manage the settings related to the LSP
 */
class ServersSettings private constructor() : Configurable {
    override fun getDisplayName(): @Nls String {
        return "Language Server Protocol"
    }

    override fun getHelpTopic(): String {
        return "com.github.gtache.lsp.settings.ServersSettings"
    }

    override fun createComponent(): JComponent {
        lspGUI = ServersGUI()
        setGUIFields(PluginMain.getExtToServerDefinition())
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
        private val logger = Logger.getInstance(ServersSettings::class.java)
        private var lspGUI: ServersGUI? = null
        var instance: ServersSettings? = null
            get() {
                if (field == null) {
                    field = ServersSettings()
                }
                return field
            }
            private set
    }
}