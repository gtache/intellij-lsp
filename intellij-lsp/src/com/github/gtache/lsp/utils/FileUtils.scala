package com.github.gtache.lsp.utils

import java.io.File
import java.net.{MalformedURLException, URI, URL}

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.{Document, Editor}
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager, TextEditor}
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
  * Various file / uri related methods
  */
object FileUtils {
  val os: OS.Value = if (System.getProperty("os.name").toLowerCase.contains("win")) OS.WINDOWS else OS.UNIX
  private val LOG: Logger = Logger.getInstance(this.getClass)

  def editorFromPsiFile(psiFile: PsiFile): Editor = {
    editorFromVirtualFile(psiFile.getVirtualFile, psiFile.getProject)
  }

  def editorFromVirtualFile(file: VirtualFile, project: Project): Editor = {
    FileEditorManager.getInstance(project).getAllEditors(file).collectFirst { case t: TextEditor => t.getEditor }.orNull
  }

  def editorFromUri(uri: String, project: Project): Editor = {
    editorFromVirtualFile(virtualFileFromURI(uri), project)
  }

  def virtualFileFromURI(uri: String): VirtualFile = {
    LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(sanitizeURI(uri))))
  }

  def sanitizeURI(uri: String): String = {
    val reconstructed: StringBuilder = StringBuilder.newBuilder
    var uriCp = new String(uri)
    if (!uri.startsWith("file:")) {
      LOG.warn("Malformed uri : " + uri)
      uri //Probably not an uri
    } else {
      uriCp = uriCp.drop(5).dropWhile(c => c == '/')
      reconstructed.append("file:///")
      if (os == OS.UNIX) {
        reconstructed.append(uriCp).toString()
      } else {
        reconstructed.append(uriCp.takeWhile(c => c != '/'))
        if (!reconstructed.endsWith(":")) {
          reconstructed.append(":")
        }
        reconstructed.append(uriCp.dropWhile(c => c != '/')).toString()
      }

    }
  }

  /**
    * Returns a file type given an editor
    *
    * @param editor The editor
    * @return The FileType
    */
  def fileTypeFromEditor(editor: Editor): FileType = {
    FileDocumentManager.getInstance().getFile(editor.getDocument).getFileType
  }

  /**
    * Transforms an editor (Document) identifier to an LSP identifier
    *
    * @param editor The editor
    * @return The TextDocumentIdentifier
    */
  def editorToLSPIdentifier(editor: Editor): TextDocumentIdentifier = {
    new TextDocumentIdentifier(editorToURIString(editor))
  }

  /**
    * Returns the URI string corresponding to an Editor (Document)
    *
    * @param editor The Editor
    * @return The URI
    */
  def editorToURIString(editor: Editor): String = {
    sanitizeURI(VFSToURI(FileDocumentManager.getInstance().getFile(editor.getDocument)))
  }

  /**
    * Transforms an URI string into a VFS file
    *
    * @param uri The uri
    * @return The virtual file
    */
  def URIToVFS(uri: String): VirtualFile = {
    val res = LocalFileSystem.getInstance().findFileByPath(new File(new URI(sanitizeURI(uri)).getPath).getAbsolutePath)
    res
  }

  /**
    * Returns the project base dir uri given an editor
    *
    * @param editor The editor
    * @return The project whose the editor belongs
    */
  def editorToProjectFolderUri(editor: Editor): String = {
    pathToUri(editorToProjectFolderPath(editor))
  }

  def editorToProjectFolderPath(editor: Editor): String = {
    new File(editor.getProject.getBasePath).getAbsolutePath
  }

  /**
    * Transforms a path into an URI string
    *
    * @param path The path
    * @return The uri
    */
  def pathToUri(path: String): String = {
    sanitizeURI(new File(path).toURI.toString)
  }

  def documentToUri(document: Document): String = {
    sanitizeURI(VFSToURI(FileDocumentManager.getInstance().getFile(document)))
  }

  /**
    * Returns the URI string corresponding to a VirtualFileSystem file
    *
    * @param file The file
    * @return the URI
    */
  def VFSToURI(file: VirtualFile): String = {
    try {
      val uri = sanitizeURI(new URL(file.getUrl).toURI.toString)
      uri
    } catch {
      case e: MalformedURLException =>
        LOG.warn(e)
        null
    }
  }

  /**
    * Object representing the OS type (Windows or Unix)
    */
  object OS extends Enumeration {
    type OS = Value
    val WINDOWS, UNIX = Value
  }

}
