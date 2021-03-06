package com.github.gtache.lsp.actions

import com.github.gtache.lsp.PluginMain
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapperImpl
import com.github.gtache.lsp.settings.gui.ComboCheckboxDialog
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.actionSystem.{ActionPlaces, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.{DumbAwareAction, Project}
import com.intellij.openapi.ui.Messages

class LSPLinkToServerAction extends DumbAwareAction {
  private val LOG: Logger = Logger.getInstance(classOf[LSPLinkToServerAction])

  override def actionPerformed(anActionEvent: AnActionEvent): Unit = {
    var editors: Array[Editor] = Array()
    val project: Project = anActionEvent.getDataContext.getData(CommonDataKeys.PROJECT)
    anActionEvent.getPlace match {
      case ActionPlaces.EDITOR_POPUP =>
        editors = Array(anActionEvent.getDataContext.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE))
      case ActionPlaces.EDITOR_TAB_POPUP =>
        val virtualFile = anActionEvent.getDataContext.getData(CommonDataKeys.VIRTUAL_FILE)
        editors = Array(FileUtils.editorFromVirtualFile(virtualFile, project))
        if (editors(0) == null) {
          val psiFile = anActionEvent.getDataContext.getData(CommonDataKeys.PSI_FILE)
          editors = Array(FileUtils.editorFromPsiFile(psiFile))
        }
      case ActionPlaces.PROJECT_VIEW_POPUP =>
        val virtualFiles = anActionEvent.getDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        editors = openClosedEditors(virtualFiles.map(v => FileUtils.VFSToURI(v)), project)
      case _ =>
        editors = Array(anActionEvent.getDataContext.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE))
        LOG.warn("Unknown place : " + anActionEvent.getPlace)
    }
    val allEditors = editors.filter(e => e != null).partition(e => LanguageServerWrapperImpl.forEditor(e).isEmpty)
    editors = allEditors._1 ++ allEditors._2
    val alreadyConnected = allEditors._2
    if (alreadyConnected.nonEmpty) {
      Messages.showWarningDialog(project, "Editor(s) " + alreadyConnected.map(e => FileDocumentManager.getInstance().getFile(e.getDocument).getName).mkString("\n") + " already connected to servers with ext " + alreadyConnected.map(e => LanguageServerWrapperImpl.forEditor(e).get.getServerDefinition.ext.toString).mkString(",") + ", will be overwritten", "Trying to connect an already connected editor")
    }
    if (editors.nonEmpty) {
      import scala.collection.JavaConverters._
      val allDefinitions = PluginMain.getExtToServerDefinition.values.toList.distinct
      val allDefinitionNames = allDefinitions.map(d => d.ext)
      val allWrappers = PluginMain.getAllServerWrappers.toList.distinct
      val allWrapperNames = allWrappers.map(w => w.getServerDefinition.ext + " : " + w.getProject.getName)
      val dialog = new ComboCheckboxDialog(project, "Link a file to a language server", allDefinitionNames.asJava, allWrapperNames.asJava)
      dialog.show()
      val exitCode = dialog.getExitCode
      if (exitCode >= 0) {
        editors.foreach(editor => {
          val fileType = FileUtils.fileTypeFromEditor(editor)
          if (PluginMain.isExtensionSupported(fileType.getDefaultExtension)) {
            val ret = Messages.showOkCancelDialog(editor.getProject,
              "This file extension" + fileType.getDefaultExtension + " is already supported by a Language Server, continue?",
              "Known extension", "Ok", "Cancel", Messages.getWarningIcon)
            if (ret == Messages.OK) {
              PluginMain.forceEditorOpened(editor, allDefinitions(exitCode), project)
            }
          } else PluginMain.forceEditorOpened(editor, allDefinitions(exitCode), project)
        })
      }
    }
  }

  private def openClosedEditors(uris: Iterable[String], project: Project): Array[Editor] = {
    uris.map(uri => {
      var editor = FileUtils.editorFromUri(uri, project)
      if (editor == null) {
        val (_, e) = FileUtils.openClosedEditor(uri, project)
        editor = e
      }
      editor
    }).toArray
  }
}