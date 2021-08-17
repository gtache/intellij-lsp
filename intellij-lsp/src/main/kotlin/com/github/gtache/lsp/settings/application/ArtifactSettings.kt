package com.github.gtache.lsp.settings.application

import com.github.gtache.lsp.settings.application.gui.ArtifactGUI
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class ArtifactSettings : Configurable {
    override fun getDisplayName(): @Nls String {
        return "Coursier"
    }

    override fun getHelpTopic(): String {
        return "com.github.gtache.lsp.settings.CoursierSettings"
    }

    override fun createComponent(): JComponent {
        coursierGUI = ArtifactGUI()
        return coursierGUI!!.getRootPanel()
    }

    override fun isModified(): Boolean {
        return coursierGUI?.isModified() ?: false
    }

    override fun apply() {
        coursierGUI?.apply()
    }

    override fun reset() {
        coursierGUI?.reset()
    }

    override fun disposeUIResources() {
        coursierGUI = null
    }

    companion object {
        private var coursierGUI: ArtifactGUI? = null
    }
}