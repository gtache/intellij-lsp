package com.github.gtache.requests

import com.github.gtache.editor.EditorEventManager
import com.github.gtache.PluginMain
import com.github.gtache.utils.Utils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile

object ReformatHandler {

  def reformatAllFiles(project: Project): Boolean = {
    var allFilesSupported = true
    ProjectFileIndex.getInstance(project).iterateContent((fileOrDir: VirtualFile) => {
      if (fileOrDir.isDirectory) {
        true
      } else {
        if (PluginMain.isExtensionSupported(fileOrDir.getExtension)) {
          reformatFile(fileOrDir, project)
          true
        } else {
          allFilesSupported = false
          true
        }
      }
    })
    allFilesSupported
  }

  def reformatFile(file: VirtualFile, project: Project): Unit = {
    val uri = Utils.VFSToURIString(file)
    EditorEventManager.forUri(uri) match {
      case Some(manager) =>
        manager.reformat()
      case None =>
        val fileEditorManager = FileEditorManager.getInstance(project)
        val descriptor = new OpenFileDescriptor(project, file)
        ApplicationManager.getApplication.invokeLater(() => ApplicationManager.getApplication.runWriteAction(new Runnable {
          override def run(): Unit = fileEditorManager.openTextEditor(descriptor, false)
        }))
        while (EditorEventManager.forUri(uri).isEmpty) {}
        EditorEventManager.forUri(uri).get.reformat()
        ApplicationManager.getApplication.invokeLater(() => ApplicationManager.getApplication.runWriteAction(new Runnable {
          override def run(): Unit = fileEditorManager.closeFile(file)
        }))
    }
  }

  def reformatFile(editor: Editor): Unit =
    EditorEventManager.forEditor(editor).foreach(manager => manager.reformat())


  def reformatSelection(editor: Editor): Unit = {
    EditorEventManager.forEditor(editor).foreach(manager => manager.reformatSelection())
  }

}
