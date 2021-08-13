package com.github.gtache.lsp.utils

import com.github.gtache.lsp.utils.ApplicationUtils.computableReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.util.TextRange
import com.intellij.util.DocumentUtil
import org.eclipse.lsp4j.Position

/**
 * Various methods to convert offsets / logical position / server position
 */
object DocumentUtils {

    private val logger: Logger = Logger.getInstance(this.javaClass)

    /**
     * Gets the line at the given offset given an editor and bolds the text between the given offsets
     *
     * @param editor      The editor
     * @param startOffset The starting offset
     * @param endOffset   The ending offset
     * @return The document line
     */
    fun getLineText(editor: Editor, startOffset: Int, endOffset: Int): String {
        return computableReadAction {
            val doc = editor.document
            val lineIdx = doc.getLineNumber(startOffset)
            val lineStartOff = doc.getLineStartOffset(lineIdx)
            val lineEndOff = doc.getLineEndOffset(lineIdx)
            val line = doc.getTextClamped(TextRange(lineStartOff, lineEndOff))
            val startOffsetInLine = startOffset - lineStartOff
            val endOffsetInLine = endOffset - lineStartOff
            line.substring(0, startOffsetInLine) + "<b>" + line.substring(startOffsetInLine, endOffsetInLine) + "</b>" + line.substring(endOffsetInLine)
        }
    }

    /**
     * Transforms a LogicalPosition (IntelliJ) to an LSP Position
     *
     * @param position the LogicalPosition
     * @param editor   The editor
     * @return the Position
     */
    fun logicalToLSPPos(position: LogicalPosition, editor: Editor): Position {
        return computableReadAction { offsetToLSPPos(editor, editor.logicalPositionToOffset(position)) }
    }

    /**
     * Calculates a Position given an editor and an offset
     *
     * @param editor The editor
     * @param offset The offset
     * @return an LSP position
     */
    fun offsetToLSPPos(editor: Editor, offset: Int): Position {
        return computableReadAction {
            val doc = editor.document
            val line = doc.getLineNumber(offset)
            val lineStart = doc.getLineStartOffset(line)
            val lineTextBeforeOffset = doc.getTextClamped(TextRange.create(lineStart, offset))
            val column = lineTextBeforeOffset.length
            Position(line, column)
        }
    }

    /**
     * Transforms an LSP position to an editor offset
     *
     * @param editor The editor
     * @param pos    The LSPPos
     * @return The offset
     */
    fun LSPPosToOffset(editor: Editor, pos: Position): Int {
        return computableReadAction {
            val doc = editor.document
            val line = 0.coerceAtLeast(pos.line.coerceAtMost(doc.lineCount - 1))
            val docLength = doc.textLength
            val offset = doc.getLineStartOffset(line) + pos.character
            if (!DocumentUtil.isValidOffset(offset, doc)) {
                logger.debug("Invalid offset : $offset, doclength $docLength")
            }
            offset.coerceAtLeast(0).coerceAtMost(docLength - 1)
        }
    }

    fun LSPRangeToTextRange(editor: Editor, range: org.eclipse.lsp4j.Range): TextRange {
        return TextRange(LSPPosToOffset(editor, range.start), LSPPosToOffset(editor, range.end))
    }

    fun expandOffsetToToken(editor: Editor, offset: Int): TextRange {
        return computableReadAction {
            val text = editor.document.text
            val negOffset = offset - text.take(offset).reversed().takeWhile { c -> c.isLetterOrDigit() || c == '_' }.length
            val posOffset = offset + text.drop(offset).takeWhile { c -> c.isLetterOrDigit() || c == '_' }.length
            TextRange(negOffset, posOffset)
        }
    }

    fun Document.getTextClamped(start: Int, end: Int): String {
        val textRange = TextRange(0.coerceAtLeast(start), (textLength - 1).coerceAtMost(end))
        return getText(textRange)
    }

    fun Document.getTextClamped(textRange: TextRange): String {
        return getTextClamped(0.coerceAtLeast(textRange.startOffset), (textLength - 1).coerceAtMost(textRange.endOffset))
    }

}