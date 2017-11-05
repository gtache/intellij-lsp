package com.github.gtache.editor.listeners

import com.github.gtache.editor.EditorEventManager
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate.Result
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class LSPTypedHandler extends TypedHandlerDelegate {
  override def charTyped(c: Char, project: Project, editor: Editor, file: PsiFile) = {
    EditorEventManager.forEditor(editor) match {
      case Some(manager) =>
        manager.characterTyped(c)
      case None =>
    }
    Result.CONTINUE
  }
}
