package com.github.gtache.actions

import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

import com.github.gtache.PluginMain
import com.github.gtache.requests.ReformatHandler
import com.intellij.codeInsight.actions.ReformatCodeAction
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiDocumentManager

class LSPReformatAction extends ReformatCodeAction {

  private val LOG: Logger = Logger.getInstance(classOf[LSPReformatAction])

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getData(CommonDataKeys.PROJECT)
    val editor = e.getData(CommonDataKeys.EDITOR)
    if (editor != null) {
      val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument)
      if (!LanguageFormatting.INSTANCE.allForLanguage(file.getLanguage).isEmpty) {
        super.actionPerformed(e)
      } else if (PluginMain.isExtensionSupported(file.getVirtualFile.getExtension)) {
        ReformatHandler.reformatFile(editor)
      }
    } else if (project != null) {
      CompletableFuture.supplyAsync(new Supplier[Boolean] {
        override def get(): Boolean = ReformatHandler.reformatAllFiles(project)
      }).thenAccept(res => if (!res) super.actionPerformed(e))
    } else {
      super.actionPerformed(e)
    }
  }

  override def update(event: AnActionEvent): Unit = super.update(event)

}
