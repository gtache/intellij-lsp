package com.github.gtache.lsp.contributors.rename

import com.github.gtache.lsp.editor.services.application.EditorApplicationService
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.NonEmptyInputValidator

/**
 * LSP application rename helper
 */
object LSPRenameHelper {

    /**
     * Renames the currently selected element in the given editor
     */
    fun rename(editor: Editor): Unit {
        val manager = service<EditorApplicationService>().managerForEditor(editor)
        if (manager != null) {
            if (manager.canRename()) {
                val renameTo = Messages.showInputDialog(editor.project, "Enter name: ", "Rename", Messages.getQuestionIcon(), "", NonEmptyInputValidator())
                if (renameTo != null && renameTo != "") manager.rename(renameTo)
            }
        }
    }
}