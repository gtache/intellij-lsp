package com.github.gtache.lsp.actions

import com.github.gtache.lsp.contributors.rename.LSPRenameHelper
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction

/**
  * Action called when the user presses SHIFT+ALT+F6 to rename a symbol
  */
class LSPRefactoringAction extends DumbAwareAction {
  private val LOG: Logger = Logger.getInstance(classOf[LSPRefactoringAction])

  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    LSPRenameHelper.rename(editor)
  }
}
