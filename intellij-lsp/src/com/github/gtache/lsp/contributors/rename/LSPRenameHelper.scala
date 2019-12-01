package com.github.gtache.lsp.contributors.rename

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.{Messages, NonEmptyInputValidator}

object LSPRenameHelper {
  def rename(editor: Editor): Unit = {
    EditorEventManager.forEditor(editor) match {
      case Some(manager) =>
        val renameTo = Messages.showInputDialog(editor.getProject, "Enter new name: ", "Rename", Messages.getQuestionIcon, "", new NonEmptyInputValidator())
        if (renameTo != null && renameTo != "") manager.rename(renameTo)
      case None =>
    }
  }
}
