package com.github.gtache.lsp.requests

import java.io.File
import java.net.URI

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import org.eclipse.lsp4j.{TextEdit, WorkspaceEdit}

/**
  * An Object handling WorkspaceEdits
  */
object WorkspaceEditHandler {

  import com.github.gtache.lsp.utils.ApplicationUtils._

  private val LOG: Logger = Logger.getInstance(WorkspaceEditHandler.getClass)

  /**
    * Applies a WorkspaceEdit
    *
    * @param edit The edit
    * @return True if everything was applied, false otherwise
    */
  def applyEdit(edit: WorkspaceEdit, name: String = "LSP edits"): Boolean = {
    import scala.collection.JavaConverters._
    val changes = edit.getChanges.asScala
    val dChanges = edit.getDocumentChanges.asScala
    var didApply: Boolean = true

    def manageUnopenedEditor(edits: Iterable[TextEdit], uri: String, version: Int = Int.MaxValue): Unit = {
      val project = ProjectManager.getInstance().getOpenProjects()(0)
      val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(uri)))
      val fileEditorManager = FileEditorManager.getInstance(project)
      val descriptor = new OpenFileDescriptor(project, file)
      invokeLater(() => {
        val editor = computableWriteAction(() => {
          fileEditorManager.openTextEditor(descriptor, false)
        })
        EditorEventManager.forEditor(editor) match {
          case Some(manager) => if (!manager.applyEdit(version, edits, name)) didApply = false
          case None => didApply = false
        }
      })
      writeAction(() => fileEditorManager.closeFile(file))
    }

    if (dChanges != null) {
      dChanges.foreach(edit => {
        val doc = edit.getTextDocument
        val version = doc.getVersion
        val uri = doc.getUri
        EditorEventManager.forUri(uri) match {
          case Some(manager) => if (!manager.applyEdit(version, edit.getEdits.asScala.toList, name)) didApply = false
          case None =>
            manageUnopenedEditor(edit.getEdits.asScala, uri, version)
        }
      })
    } else {
      changes.foreach(edit => {
        val uri = edit._1
        val changes = edit._2.asScala
        EditorEventManager.forUri(uri) match {
          case Some(manager) => if (!manager.applyEdit(edits = changes.toList, name = name)) didApply = false
          case None =>
            manageUnopenedEditor(changes, uri)
        }
      })
    }
    didApply
  }

}
