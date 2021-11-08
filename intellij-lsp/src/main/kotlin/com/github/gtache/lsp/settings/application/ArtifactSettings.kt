package com.github.gtache.lsp.settings.application

import com.github.gtache.lsp.settings.application.gui.ArtifactGUI
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Settings for artifact retrieval
 */
class ArtifactSettings : Configurable {
    override fun getDisplayName(): @Nls String {
        return "Aether"
    }

    override fun getHelpTopic(): String {
        return "com.github.gtache.lsp.settings.ArtifactSettings"
    }

    override fun createComponent(): JComponent {
        artifactGUI = ArtifactGUI()
        return artifactGUI!!.getRootPanel()
    }

    override fun isModified(): Boolean {
        return artifactGUI?.isModified() ?: false
    }

    override fun apply() {
        artifactGUI?.apply()
    }

    override fun reset() {
        artifactGUI?.reset()
    }

    override fun disposeUIResources() {
        artifactGUI = null
    }

    companion object {
        private var artifactGUI: ArtifactGUI? = null
    }
}