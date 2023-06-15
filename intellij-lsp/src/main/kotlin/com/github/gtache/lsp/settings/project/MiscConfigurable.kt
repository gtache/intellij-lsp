package com.github.gtache.lsp.settings.project

import com.github.gtache.lsp.settings.project.gui.MiscSettingsGUI
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Miscellaneous settings
 */
class MiscConfigurable(private val project: Project) : Configurable {
    override fun getDisplayName(): @Nls String {
        return "Language Server Protocol"
    }

    override fun getHelpTopic(): String {
        return "com.github.gtache.lsp.settings.project.MiscSettings"
    }

    override fun createComponent(): JComponent {
        val gui = MiscSettingsGUI(project)
        miscGUI = gui
        return gui.getRootPanel()
    }

    override fun isModified(): Boolean {
        return miscGUI?.isModified() ?: false
    }

    override fun apply() {
        miscGUI?.apply()
    }

    override fun reset() {
        miscGUI?.reset()
    }

    override fun disposeUIResources() {
        miscGUI = null
    }

    companion object {
        private var miscGUI: MiscSettingsGUI? = null
    }
}