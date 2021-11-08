package com.github.gtache.lsp.settings.application

import com.github.gtache.lsp.settings.application.gui.TimeoutGUI
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Settings for the Timeouts
 */
class TimeoutSettings private constructor() : Configurable {
    override fun getDisplayName(): @Nls String {
        return "Timeouts"
    }

    override fun getHelpTopic(): String {
        return "com.github.gtache.lsp.settings.application.TimeoutSettings"
    }

    override fun createComponent(): JComponent {
        timeoutGUI = TimeoutGUI()
        return timeoutGUI!!.getRootPanel()
    }

    override fun isModified(): Boolean {
        return timeoutGUI?.isModified() ?: false
    }

    override fun apply() {
        timeoutGUI?.apply()
    }

    override fun reset() {
        timeoutGUI?.reset()
    }

    override fun disposeUIResources() {
        timeoutGUI = null
    }

    companion object {
        private var timeoutGUI: TimeoutGUI? = null
    }
}