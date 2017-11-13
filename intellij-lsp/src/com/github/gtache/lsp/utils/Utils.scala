package com.github.gtache.lsp.utils

import java.io.File
import java.net.{MalformedURLException, URI, URL}

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import org.eclipse.lsp4j.{Position, TextDocumentIdentifier}

import scala.annotation.varargs

/**
  * Object containing some useful methods for the plugin
  */
object Utils {

  val os: OS.Value = if (System.getProperty("os.name").toLowerCase.contains("win")) OS.WINDOWS else OS.UNIX
  private val LOG: Logger = Logger.getInstance(Utils.getClass)

  /**
    * Calculates a Position given an editor and an offset
    *
    * @param editor The editor
    * @param offset The offset
    * @return an LSP position
    */
  def offsetToLSPPos(editor: Editor, offset: Int): Position = {
    logicalToLSPPos(editor.offsetToLogicalPosition(offset))
  }

  /**
    * Transforms a LogicalPosition (IntelliJ) to an LSP Position
    *
    * @param position the LogicalPosition
    * @return the Position
    */
  def logicalToLSPPos(position: LogicalPosition): Position = {
    new Position(position.line, position.column)
  }

  /**
    * Transforms an LSP position to an editor offset
    *
    * @param editor The editor
    * @param pos    The LSPPos
    * @return The offset
    */
  def LSPPosToOffset(editor: Editor, pos: Position): Int = {
    math.min(math.max(editor.logicalPositionToOffset(LSPToLogicalPos(pos)), 0), editor.getDocument.getTextLength - 1)
  }

  /**
    * Transforms an LSP position to a LogicalPosition
    *
    * @param position The LSPPos
    * @return The LogicalPos
    */
  def LSPToLogicalPos(position: Position): LogicalPosition = {
    new LogicalPosition(position.getLine, position.getCharacter)
  }

  /**
    * Transforms an array into a string (using mkString, useful for Java)
    *
    * @param arr The array
    * @param sep A separator
    * @return The result of mkString
    */
  def arrayToString(arr: Array[Any], sep: String = ""): String = {
    arr.mkString(sep)
  }

  @varargs def concatenateArrays(arr: Array[Any]*): Array[Any] = {
    arr.flatten.toArray
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
    sanitizeURI(VFSToURIString(FileDocumentManager.getInstance().getFile(editor.getDocument)))
  }

  /**
    * Returns the URI string corresponding to a VirtualFileSystem file
    *
    * @param file The file
    * @return the URI
    */
  def VFSToURIString(file: VirtualFile): String = {
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

  private def sanitizeURI(uri: String): String = {
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

  object OS extends Enumeration {
    type OS = Value
    val WINDOWS, UNIX = Value
  }

}
