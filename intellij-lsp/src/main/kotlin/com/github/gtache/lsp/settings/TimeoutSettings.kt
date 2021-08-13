package com.github.gtache.lsp.settings

import com.github.gtache.lsp.settings.gui.TimeoutGUI
import com.intellij.openapi.diagnostic.Logger
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
        return "com.github.gtache.lsp.settings.TimeoutSettings"
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
        private val logger = Logger.getInstance(
            TimeoutSettings::class.java
        )
        private var timeoutGUI: TimeoutGUI? = null
        var instance: TimeoutSettings? = null
            get() {
                if (field == null) {
                    field = TimeoutSettings()
                }
                return field
            }
            private set
    }
}