package com.github.gtache.lsp.actions

import com.github.gtache.lsp.PluginMain
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages

class LSPAddToServerFilesAction extends DumbAwareAction {
  override def actionPerformed(anActionEvent: AnActionEvent): Unit = {
    val editor = anActionEvent.getDataContext.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE)
    if (editor != null) {
      val fileType = FileUtils.fileTypeFromEditor(editor)
      if (PluginMain.isExtensionSupported(fileType.getDefaultExtension)) {
        val ret = Messages.showOkCancelDialog(editor.getProject,
          "This file extension is already supported by a Language Server, continue?",
          "Known extension", Messages.getWarningIcon)
        if (ret == Messages.CANCEL) {
          return
        }
      }
      val allDefinitions = PluginMain.getExtToServerDefinition.values.toArray
      val allNames = allDefinitions.map(d => d.ext)
      //Messages.showCheckboxMessageDialog()
    }
  }
}
