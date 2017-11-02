package com.github.gtache.requests

import java.io.File
import java.net.URI

import com.github.gtache.PluginMain
import com.github.gtache.editor.EditorEventManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import org.eclipse.lsp4j.WorkspaceEdit

object WorkspaceEditHandler {

  private val LOG: Logger = Logger.getInstance(WorkspaceEditHandler.getClass)

  def applyEdit(edit: WorkspaceEdit): Boolean = {
    import scala.collection.JavaConverters._
    val changes = edit.getChanges.asScala
    val dChanges = edit.getDocumentChanges.asScala
    var didApply: Boolean = true
    if (dChanges != null) {
      dChanges.foreach(edit => {
        val doc = edit.getTextDocument
        val version = doc.getVersion
        val uri = doc.getUri
        EditorEventManager.forUri(uri) match {
          case Some(manager) => if (!manager.applyEdit(version, edit.getEdits.asScala.toList)) didApply = false
          case None =>
            val project = ProjectManager.getInstance().getOpenProjects()(0)
            val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(uri)))
            if (PluginMain.isExtensionSupported(file.getExtension)) {
              val fileEditorManager = FileEditorManager.getInstance(project)
              val descriptor = new OpenFileDescriptor(project, file)
              ApplicationManager.getApplication.invokeLater(() => ApplicationManager.getApplication.runWriteAction(new Runnable {
                override def run(): Unit = fileEditorManager.openTextEditor(descriptor, false)
              }))
              while (EditorEventManager.forUri(uri).isEmpty) {}
              if (!EditorEventManager.forUri(uri).get.applyEdit(version, edit.getEdits.asScala.toList)) didApply = false
              ApplicationManager.getApplication.invokeLater(() => ApplicationManager.getApplication.runWriteAction(new Runnable {
                override def run(): Unit = fileEditorManager.closeFile(file)
              }))
            }
        } else {
          LOG.warn("Unsupported file ext sent by server : "+uri)
        }
      })
    } else {
      changes.foreach(edit => {
        val uri = edit._1
        val changes = edit._2.asScala
        EditorEventManager.forUri(uri) match {
          case Some(manager) => if (!manager.applyEdit(edits = changes.toList)) didApply = false
          case None =>
            val project = ProjectManager.getInstance().getOpenProjects()(0)
            val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(uri)))
            if (PluginMain.isExtensionSupported(file.getExtension)) { //Should always be true
              val fileEditorManager = FileEditorManager.getInstance(project)
              val descriptor = new OpenFileDescriptor(project, file)
              ApplicationManager.getApplication.invokeLater(() => ApplicationManager.getApplication.runWriteAction(new Runnable {
                override def run(): Unit = fileEditorManager.openTextEditor(descriptor, false)
              }))
              while (EditorEventManager.forUri(uri).isEmpty) {}
              if (!EditorEventManager.forUri(uri).get.applyEdit(edits = changes.toList)) didApply = false
              ApplicationManager.getApplication.invokeLater(() => ApplicationManager.getApplication.runWriteAction(new Runnable {
                override def run(): Unit = fileEditorManager.closeFile(file)
              }))
            } else {
              LOG.warn("Unsupported file ext sent by server : "+uri)
            }
        }
      })
    }
    didApply
  }

}
