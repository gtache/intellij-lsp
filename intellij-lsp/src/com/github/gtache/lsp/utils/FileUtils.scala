package com.github.gtache.lsp.utils

import java.io.File
import java.net.{URI, URL}

import com.github.gtache.lsp.utils.ApplicationUtils.computableWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.{Document, Editor}
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager, OpenFileDescriptor, TextEditor}
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.{Project, ProjectUtil}
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
  * Various file / uri related methods
  */
object FileUtils {
  val os: OS.Value = if (System.getProperty("os.name").toLowerCase.contains("win")) OS.WINDOWS else OS.UNIX
  val COLON_ENCODED: String = "%3A"
  val SPACE_ENCODED: String = "%20"
  val URI_FILE_BEGIN: String = "file:"
  val URI_VALID_FILE_BEGIN: String = URI_FILE_BEGIN + "///"
  val URI_PATH_SEP: Char = '/'

  val LSP_ROOT_DIR: String = "lsp/"
  val LSP_LOG_DIR: String = LSP_ROOT_DIR + "log/"
  val LSP_CONFIG_DIR: String = LSP_ROOT_DIR + "conf/"
  private val LOG: Logger = Logger.getInstance(this.getClass)

  def extFromPsiFile(psiFile: PsiFile): String = {
    psiFile.getVirtualFile.getExtension
  }

  def editorFromPsiFile(psiFile: PsiFile): Editor = {
    editorFromVirtualFile(psiFile.getVirtualFile, psiFile.getProject)
  }

  def editorFromUri(uri: String, project: Project): Editor = {
    editorFromVirtualFile(URIToVFS(uri), project)
  }

  def editorFromVirtualFile(file: VirtualFile, project: Project): Editor = {
    FileEditorManager.getInstance(project).getAllEditors(file).collectFirst { case t: TextEditor => t.getEditor }.orNull
  }

  def openClosedEditor(uri: String, project: Project): (VirtualFile, Editor) = {
    val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(FileUtils.sanitizeURI(uri))))
    val fileEditorManager = FileEditorManager.getInstance(project)
    val descriptor = new OpenFileDescriptor(project, file)
    val editor: Editor = computableWriteAction(() => {
      fileEditorManager.openTextEditor(descriptor, false)
    })
    (file, editor)
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

  def extFromEditor(editor: Editor): String = {
    FileDocumentManager.getInstance().getFile(editor.getDocument).getExtension
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
    * Returns the URI string corresponding to a VirtualFileSystem file
    *
    * @param file The file
    * @return the URI
    */
  def VFSToURI(file: VirtualFile): String = {
    if (file != null) {
      try {
        sanitizeURI(new URL(file.getUrl.replace(" ", SPACE_ENCODED)).toURI.toString)
      } catch {
        case e: Exception =>
          LOG.warn(e)
          LOG.warn("Caused by " + file.getUrl)
          null
      }
    } else null
  }

  /**
    * Fixes common problems in uri, mainly related to Windows
    *
    * @param uri The uri to sanitize
    * @return The sanitized uri
    */
  def sanitizeURI(uri: String): String = {
    if (uri != null) {
      val reconstructed: StringBuilder = StringBuilder.newBuilder
      var uriCp = new String(uri).replace(" ", SPACE_ENCODED) //Don't trust servers
      if (!uri.startsWith(URI_FILE_BEGIN)) {
        LOG.warn("Malformed uri : " + uri)
        uri //Probably not an uri
      } else {
        uriCp = uriCp.drop(URI_FILE_BEGIN.length).dropWhile(c => c == URI_PATH_SEP)
        reconstructed.append(URI_VALID_FILE_BEGIN)
        if (os == OS.UNIX) {
          reconstructed.append(uriCp).toString()
        } else {
          reconstructed.append(uriCp.takeWhile(c => c != URI_PATH_SEP))
          val driveLetter = reconstructed.charAt(URI_VALID_FILE_BEGIN.length)
          if (driveLetter.isLower) {
            reconstructed.setCharAt(URI_VALID_FILE_BEGIN.length, driveLetter.toUpper)
          }
          if (reconstructed.endsWith(COLON_ENCODED)) {
            reconstructed.delete(reconstructed.length - 3, reconstructed.length)
          }
          if (!reconstructed.endsWith(":")) {
            reconstructed.append(":")
          }
          reconstructed.append(uriCp.dropWhile(c => c != URI_PATH_SEP)).toString()
        }

      }
    } else null
  }

  /**
    * Transforms an URI string into a VFS file
    *
    * @param uri The uri
    * @return The virtual file
    */
  def URIToVFS(uri: String): VirtualFile = {
    LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(sanitizeURI(uri))))
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
    val project = editor.getProject
    if (project != null && !project.isDefault) {
      VFSToPath(ProjectUtil.guessProjectDir(project))
    } else null
  }

  def VFSToPath(file: VirtualFile): String = {
    new File(file.getPath).getAbsolutePath
  }

  /**
    * Transforms a path into an URI string
    *
    * @param path The path
    * @return The uri
    */
  def pathToUri(path: String): String = {
    sanitizeURI(new File(path.replace(" ", SPACE_ENCODED)).toURI.toString)
  }

  def uriToPath(uri: String): String = {
    new File(new URI(uri)).getAbsolutePath
  }

  def projectToUri(project: Project): String = {
    pathToUri(new File(project.getBasePath).getAbsolutePath)
  }

  def documentToUri(document: Document): String = {
    sanitizeURI(VFSToURI(FileDocumentManager.getInstance().getFile(document)))
  }

  def getAllOpenedEditors(project: Project): Seq[Editor] = {
    ApplicationUtils.computableReadAction(() => FileEditorManager.getInstance(project).getAllEditors().filter(e => e.isInstanceOf[TextEditor]).map(e => e.asInstanceOf[TextEditor].getEditor).toSeq)
  }

  /**
    * Object representing the OS type (Windows or Unix)
    */
  object OS extends Enumeration {
    type OS = Value
    val WINDOWS, UNIX = Value
  }

}
