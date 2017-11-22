package com.github.gtache.lsp.requests

import java.io.File
import java.net.URI

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.command.{CommandProcessor, UndoConfirmationPolicy}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
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

    invokeLater(() => {
      var curProject: Project = null
      val openedEditors: scala.collection.mutable.ListBuffer[VirtualFile] = scala.collection.mutable.ListBuffer()

      def manageUnopenedEditor(edits: Iterable[TextEdit], uri: String, version: Int = Int.MaxValue): Runnable = {
        val project = ProjectManager.getInstance().getOpenProjects()(0)
        val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(uri)))
        val fileEditorManager = FileEditorManager.getInstance(project)
        val descriptor = new OpenFileDescriptor(project, file)
        val editor: Editor = computableWriteAction(() => {
          fileEditorManager.openTextEditor(descriptor, false)
        })
        openedEditors += file
        curProject = editor.getProject
        var runnable: Runnable = null
        EditorEventManager.forEditor(editor) match {
          case Some(manager) => runnable = manager.getEditsRunnable(version, edits, name)
          case None =>
        }
        runnable
      }

      val toApply: scala.collection.mutable.ListBuffer[Runnable] = scala.collection.mutable.ListBuffer()
      if (dChanges != null) {
        dChanges.foreach(edit => {
          val doc = edit.getTextDocument
          val version = doc.getVersion
          val uri = doc.getUri
          toApply += (EditorEventManager.forUri(uri) match {
            case Some(manager) =>
              curProject = manager.editor.getProject
              manager.getEditsRunnable(version, edit.getEdits.asScala.toList, name)
            case None => manageUnopenedEditor(edit.getEdits.asScala, uri, version)
          })
        })
      } else {
        changes.foreach(edit => {
          val uri = edit._1
          val changes = edit._2.asScala
          toApply += (EditorEventManager.forUri(uri) match {
            case Some(manager) =>
              curProject = manager.editor.getProject
              manager.getEditsRunnable(edits = changes.toList, name = name)
            case None =>
              manageUnopenedEditor(changes, uri)
          })
        })
      }
      if (toApply.contains(null)) {
        LOG.warn("Didn't apply, null runnable")
        didApply = false
      } else {
        val runnable = new Runnable {
          override def run(): Unit = toApply.foreach(r => r.run())
        }
        writeAction(() => {
          CommandProcessor.getInstance().executeCommand(curProject, runnable, name, "LSPPlugin", UndoConfirmationPolicy.DEFAULT, false)
          openedEditors.foreach(f => FileEditorManager.getInstance(curProject).closeFile(f))
        })
      }

    })
    didApply
  }

}
