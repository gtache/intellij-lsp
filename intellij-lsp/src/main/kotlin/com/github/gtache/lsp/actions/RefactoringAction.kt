package com.github.gtache.lsp.actions

import com.github.gtache.lsp.contributors.rename.LSPRenameHelper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction

/**
 * Action called when the user presses SHIFT+ALT+F6 to rename a symbol
 */
class RefactoringAction : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent): Unit {
        val editor = e.getData(CommonDataKeys.EDITOR)
        editor?.let { LSPRenameHelper.rename(it) }
    }
}