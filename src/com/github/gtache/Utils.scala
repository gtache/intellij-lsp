package com.github.gtache

import java.io.File
import java.net.URL

import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.{Position, TextDocumentIdentifier}

/**
  * Object containing some useful methods for the plugin
  */
object Utils {

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
    new URL(FileDocumentManager.getInstance().getFile(editor.getDocument).getUrl).toURI.toString
  }

  /**
    * Returns the URI string corresponding to a VirtualFileSystem file
    *
    * @param file The file
    * @return the URI
    */
  def VFSToURIString(file: VirtualFile): String = {
    new URL(file.getUrl).toURI.toString
  }

  /**
    * Returns the project path given an editor
    *
    * @param editor The editor
    * @return The project whose belongs the editor
    */
  def editorToProjectFolderPath(editor: Editor): String = {
    new File(editor.getProject.getBaseDir.getPath).getAbsolutePath
  }

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
    editor.logicalPositionToOffset(LSPToLogicalPos(pos))
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
    * Returns a file type given an editor
    *
    * @param editor The editor
    * @return The FileType
    */
  def fileTypeFromEditor(editor: Editor): FileType = {
    FileDocumentManager.getInstance().getFile(editor.getDocument).getFileType
  }

}
