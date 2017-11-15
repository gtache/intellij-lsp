package com.github.gtache.lsp.actions

import com.github.gtache.lsp.PluginMain
import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.find.actions.FindUsagesAction
import com.intellij.lang.findUsages.LanguageFindUsages
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiDocumentManager

/**
  * Action for references / see usages (SHIFT+ALT+F7)
  */
class LSPReferencesAction extends DumbAwareAction{

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getData(CommonDataKeys.PROJECT)
    val editor = e.getData(CommonDataKeys.EDITOR)
    if (editor != null) {
      val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument)
      if (LanguageFindUsages.INSTANCE.allForLanguage(file.getLanguage).isEmpty && PluginMain.isExtensionSupported(file.getVirtualFile.getExtension)) {
        EditorEventManager.forEditor(editor).foreach(manager => manager.showReferences())
      }
    }
  }

}
