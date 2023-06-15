package com.github.gtache.lsp.settings.application

import com.github.gtache.lsp.settings.application.gui.ArtifactSettingsGUI
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Settings for artifact retrieval
 */
class ArtifactConfigurable : Configurable {
    override fun getDisplayName(): @Nls String {
        return "Aether"
    }

    override fun getHelpTopic(): String {
        return "com.github.gtache.lsp.settings.ArtifactSettings"
    }

    override fun createComponent(): JComponent {
        val gui = ArtifactSettingsGUI();
        artifactGUI = gui
        return gui.getRootPanel()
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
        private var artifactGUI: ArtifactSettingsGUI? = null
    }
}