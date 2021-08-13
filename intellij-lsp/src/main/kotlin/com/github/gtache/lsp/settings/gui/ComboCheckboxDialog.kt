package com.github.gtache.lsp.settings.gui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class ComboCheckboxDialog(project: Project, title: String, serverDefinitions: List<String>, serverWrappers: List<String>) :
    DialogWrapper(project, false, IdeModalityType.PROJECT) {
    private val contentPanelWrapper: ComboCheckboxDialogContentPanel
    private var exitCode: Int
    override fun createCenterPanel(): JComponent {
        return contentPanelWrapper.rootPane
    }

    override fun doOKAction() {
        exitCode = contentPanelWrapper.comboBoxIndex
        super.doOKAction()
    }

    override fun doCancelAction() {
        exitCode = -1
        super.doCancelAction()
    }

    override fun getExitCode(): Int {
        return exitCode
    }

    init {
        contentPanelWrapper = ComboCheckboxDialogContentPanel(serverDefinitions.toList(), serverWrappers.toList())
        exitCode = -1
        setTitle(title)
        init()
    }
}