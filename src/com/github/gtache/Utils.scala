package com.github.gtache

import java.net.URL

import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.{Position, TextDocumentIdentifier}

object Utils {

  def logicalToLspPos(position: LogicalPosition): Position = {
    new Position(position.line - 1, position.column)
  }

  def editorToLSPIdentifier(editor: Editor): TextDocumentIdentifier = {
    new TextDocumentIdentifier(editorToURIString(editor))
  }

  def editorToURIString(editor: Editor): String = {
    new URL(FileDocumentManager.getInstance().getFile(editor.getDocument).getUrl).toURI.toString
  }

  def VFSToURIString(file: VirtualFile): String = {
    new URL(file.getUrl).toURI.toString
  }

}
