package com.github.gtache.actions

import com.github.gtache.editor.EditorEventManager
import com.intellij.lang.LanguageRefactoringSupport
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages

class LSPRefactoringAction extends DumbAwareAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val psiFile = e.getData(CommonDataKeys.PSI_FILE)
    val supported = LanguageRefactoringSupport.INSTANCE.allForLanguage(psiFile.getLanguage)
    if (supported.isEmpty) {
      EditorEventManager.forEditor(editor) match {
        case Some(manager) =>
          val renameTo = Messages.showInputDialog(e.getProject, "Enter new name: ", "Rename", Messages.getQuestionIcon)
          manager.rename(renameTo)
        case None =>
      }
    } //else pass to default refactoring
  }

  private def LOG: Logger = Logger.getInstance(classOf[LSPRefactoringAction])
}
