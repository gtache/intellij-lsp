package com.github.gtache.lsp.settings.project

import com.github.gtache.lsp.settings.project.gui.MiscGUI
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class MiscSettings(private val project: Project) : Configurable {
    override fun getDisplayName(): @Nls String {
        return "Language Server Protocol"
    }

    override fun getHelpTopic(): String {
        return "com.github.gtache.lsp.settings.project.MiscSettings"
    }

    override fun createComponent(): JComponent {
        miscGUI = MiscGUI(project)
        return miscGUI!!.getRootPanel()
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
        private val logger = Logger.getInstance(
            MiscSettings::class.java
        )
        private var miscGUI: MiscGUI? = null
    }
}