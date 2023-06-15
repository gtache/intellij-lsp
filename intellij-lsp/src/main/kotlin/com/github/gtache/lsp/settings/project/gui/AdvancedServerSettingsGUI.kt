package com.github.gtache.lsp.settings.project.gui

import com.github.gtache.lsp.languageserver.settings.Settings
import com.github.gtache.lsp.languageserver.settings.SettingsImpl
import com.github.gtache.lsp.services.project.LSPProjectService
import com.github.gtache.lsp.settings.gui.LSPGUI
import com.github.gtache.lsp.settings.project.LSPPersistentProjectSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.uiDesigner.core.GridConstraints
import java.awt.GridLayout
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTextField

class AdvancedServerSettingsGUI(project: Project, id: String) : LSPGUI {
    private val alwaysSendRequestsCheckBox: JCheckBox = CheckBox("Always send requests", false, "Send requests even if a language plugin already supports it")
    private val logServersCommunicationsCheckBox: JCheckBox = CheckBox(
        "Log server communication",
        false,
        "Logs the server communication to the specified directory"
    )
    private val rootPanel: JPanel = createRootPanel()
    private val logDir: JTextField = JBTextField()

    init {
        logDir.toolTipText = "Directory to save the logs to"
    }

    private val settings = project.service<LSPPersistentProjectSettings>()
    private val serverSettings = settings.projectState.idToSettings[id]
    private val service = project.service<LSPProjectService>()

    override fun isModified(): Boolean {
        return serverSettings != getCurrentSettings()
    }

    override fun reset() {
        alwaysSendRequestsCheckBox.isSelected = serverSettings?.isAlwaysSendRequests ?: false
        logServersCommunicationsCheckBox.isSelected = serverSettings?.isLogging ?: false
        logDir.text = serverSettings?.logDir ?: ""
    }

    override fun apply() {
        TODO("Not yet implemented")
    }

    override fun getRootPanel(): JPanel {
        return rootPanel
    }

    private fun getCurrentSettings(): Settings {
        return SettingsImpl(logServersCommunicationsCheckBox.isSelected, logDir.text, alwaysSendRequestsCheckBox.isSelected)
    }

    private fun createRootPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>()
        panel.layout = GridLayout(2, 2)
        panel.add(
            logServersCommunicationsCheckBox, GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        )
        panel.add(
            logDir, GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        )
        panel.add(
            alwaysSendRequestsCheckBox,
            GridConstraints(
                1,
                0,
                1,
                2,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        )
        return panel
    }
}