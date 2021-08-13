package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * This class notifies an EditorEventManager that a character has been typed in the editor
 */
class LSPTypedHandler : TypedHandlerDelegate() {
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        EditorEventManager.forEditor(editor)?.characterTyped(c)
        return Result.CONTINUE
    }
}