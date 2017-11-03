package com.github.gtache.actions

import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

import com.github.gtache.PluginMain
import com.github.gtache.editor.EditorEventManager
import com.github.gtache.requests.ReformatHandler
import com.intellij.codeInsight.actions.{LayoutCodeDialog, ShowReformatFileDialog, TextRangeType}
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager

class LSPShowReformatDialogAction extends ShowReformatFileDialog {
  private val HELP_ID = "editing.codeReformatting"

  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val project = e.getData(CommonDataKeys.PROJECT)
    if (editor == null && project != null) {
      CompletableFuture.supplyAsync(new Supplier[Boolean] {
        override def get(): Boolean = ReformatHandler.reformatAllFiles(project)
      }).thenAccept(res => if (!res) super.actionPerformed(e))
    } else if (editor != null) {
      val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument)
      if (LanguageFormatting.INSTANCE.allForLanguage(file.getLanguage).isEmpty && PluginMain.isExtensionSupported(FileDocumentManager.getInstance().getFile(editor.getDocument).getExtension)) {
        val hasSelection = editor.getSelectionModel.hasSelection
        val dialog = new LayoutCodeDialog(project, file, hasSelection, HELP_ID)
        dialog.show()

        if (dialog.isOK) {
          val options = dialog.getRunOptions
          EditorEventManager.forEditor(editor).foreach(manager => if (options.getTextRangeType == TextRangeType.SELECTED_TEXT) manager.reformatSelection() else manager.reformat())
        }
      } else {
        super.actionPerformed(e)
      }
    } else {
      super.actionPerformed(e)
    }
  }

  override def update(event: AnActionEvent): Unit = super.update(event)
}
