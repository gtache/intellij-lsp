package com.github.gtache.lsp.utils

import com.github.gtache.lsp.utils.ApplicationUtils.computableReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.DocumentUtil
import org.eclipse.lsp4j.Position

import scala.math.min

/**
  * Various methods to convert offsets / logical position / server position
  */
object DocumentUtils {

  private val LOG: Logger = Logger.getInstance(this.getClass)

  /**
    * Gets the line at the given offset given an editor and bolds the text between the given offsets
    *
    * @param editor      The editor
    * @param startOffset The starting offset
    * @param endOffset   The ending offset
    * @return The document line
    */
  def getLineText(editor: Editor, startOffset: Int, endOffset: Int): String = {
    computableReadAction(() => {
      val doc = editor.getDocument
      val lineIdx = doc.getLineNumber(startOffset)
      val lineStartOff = doc.getLineStartOffset(lineIdx)
      val lineEndOff = doc.getLineEndOffset(lineIdx)
      val line = doc.getText(new TextRange(lineStartOff, lineEndOff))
      val startOffsetInLine = startOffset - lineStartOff
      val endOffsetInLine = endOffset - lineStartOff
      line.substring(0, startOffsetInLine) + "<b>" + line.substring(startOffsetInLine, endOffsetInLine) + "</b>" + line.substring(endOffsetInLine)
    })
  }

  /**
    * Transforms a LogicalPosition (IntelliJ) to an LSP Position
    *
    * @param position the LogicalPosition
    * @param editor   The editor
    * @return the Position
    */
  def logicalToLSPPos(position: LogicalPosition, editor: Editor): Position = {
    computableReadAction(() => offsetToLSPPos(editor, editor.logicalPositionToOffset(position)))
  }

  /**
    * Calculates a Position given an editor and an offset
    *
    * @param editor The editor
    * @param offset The offset
    * @return an LSP position
    */
  def offsetToLSPPos(editor: Editor, offset: Int): Position = {
    computableReadAction(() => {
      val position = editor.offsetToLogicalPosition(offset)
      new Position(position.line, position.column)
    })
  }

  /**
    * Transforms an LSP position to an editor offset
    *
    * @param editor The editor
    * @param pos    The LSPPos
    * @return The offset
    */
  def LSPPosToOffset(editor: Editor, pos: Position): Int = {
    computableReadAction(() => {
      editor.logicalPositionToOffset(new LogicalPosition(pos.getLine, pos.getCharacter))
    })
  }

}
