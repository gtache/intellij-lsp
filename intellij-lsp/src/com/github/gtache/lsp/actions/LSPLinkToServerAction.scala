package com.github.gtache.lsp.actions

import com.github.gtache.lsp.PluginMain
import com.github.gtache.lsp.settings.gui.ComboCheckboxDialog
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages

class LSPLinkToServerAction extends DumbAwareAction {
  override def actionPerformed(anActionEvent: AnActionEvent): Unit = {
    var editor = anActionEvent.getDataContext.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE)
    if (editor == null) {
      val psiFile = anActionEvent.getDataContext.getData(CommonDataKeys.PSI_FILE)
      editor = FileUtils.editorFromPsiFile(psiFile)
    }
    if (editor == null) {
      val virtualFile = anActionEvent.getDataContext.getData(CommonDataKeys.VIRTUAL_FILE)
      editor = FileUtils.editorFromVirtualFile(virtualFile, anActionEvent.getDataContext.getData(CommonDataKeys.PROJECT))
    }
    if (editor != null) {
      val fileType = FileUtils.fileTypeFromEditor(editor)
      if (PluginMain.isExtensionSupported(fileType.getDefaultExtension)) {
        val ret = Messages.showOkCancelDialog(editor.getProject,
          "This file extension is already supported by a Language Server, continue?",
          "Known extension", Messages.getWarningIcon)
        if (ret == Messages.OK) {
          linkEditor(editor)
        }
      } else {
        linkEditor(editor)
      }
    }
  }

  private def linkEditor(editor: Editor): Unit = {
    import scala.collection.JavaConverters._
    val allDefinitions = PluginMain.getExtToServerDefinition.values.toList.distinct
    val allDefinitionNames = allDefinitions.map(d => d.ext)
    val allWrappers = PluginMain.getAllServerWrappers.toList.distinct
    val allWrapperNames = allWrappers.map(w => w.getServerDefinition.ext + " : " + w.getProject.getName)
    val dialog = new ComboCheckboxDialog(editor.getProject, "Link a file to a language server", allDefinitionNames.asJava, allWrapperNames.asJava)
    dialog.show()
    val exitCode = dialog.getExitCode
    if (exitCode >= allDefinitionNames.size) {
      val wrapper = allWrappers(exitCode - allDefinitionNames.size)
      PluginMain.forceEditorOpened(editor, wrapper.getServerDefinition, wrapper)
    } else if (exitCode >= 0) {
      PluginMain.forceEditorOpened(editor, allDefinitions(exitCode))
    }
  }
}