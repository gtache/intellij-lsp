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
     * Returns the line at the given [startOffset] given an [editor] with the text between [startOffset] and [endOffset] in bold
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
     * Transforms a logical [position] (IntelliJ) in an [editor] to an LSP Position
     */
    fun logicalPositionToLSPPosition(position: LogicalPosition, editor: Editor): Position {
        return computableReadAction { offsetToLSPPosition(editor, editor.logicalPositionToOffset(position)) }
    }

    /**
     * Calculates an LSP Position given an [editor] and an [offset]
     */
    fun offsetToLSPPosition(editor: Editor, offset: Int): Position {
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
     * Transforms an LSP [position] to an [editor] offset
     */
    fun lspPositionToOffset(editor: Editor, position: Position): Int {
        return computableReadAction {
            val doc = editor.document
            val line = 0.coerceAtLeast(position.line.coerceAtMost(doc.lineCount - 1))
            val docLength = doc.textLength
            val offset = doc.getLineStartOffset(line) + position.character
            if (!DocumentUtil.isValidOffset(offset, doc)) {
                logger.debug("Invalid offset : $offset, doclength $docLength")
            }
            offset.coerceAtLeast(0).coerceAtMost(docLength - 1)
        }
    }

    /**
     * Transforms an LSP [range] to an [editor] range
     */
    fun lspRangeToTextRange(editor: Editor, range: org.eclipse.lsp4j.Range): TextRange {
        return TextRange(lspPositionToOffset(editor, range.start), lspPositionToOffset(editor, range.end))
    }

    /**
     * Expands the given [offset] in an [editor] to its whole token
     */
    fun expandOffsetToToken(editor: Editor, offset: Int): TextRange {
        return computableReadAction {
            val text = editor.document.text
            val negOffset = offset - text.take(offset).reversed().takeWhile { c -> c.isLetterOrDigit() || c == '_' }.length
            val posOffset = offset + text.drop(offset).takeWhile { c -> c.isLetterOrDigit() || c == '_' }.length
            TextRange(negOffset, posOffset)
        }
    }

    /**
     * Clamps the text from max(0, [start]) to min(document length, [end])
     */
    fun Document.getTextClamped(start: Int, end: Int): String {
        val textRange = TextRange(0.coerceAtLeast(start), (textLength - 1).coerceAtMost(end))
        return getText(textRange)
    }

    /**
     * Clamps the text from max(0, [textRange] start) to min(document length, [textRange] end)
     */
    fun Document.getTextClamped(textRange: TextRange): String {
        return getTextClamped(0.coerceAtLeast(textRange.startOffset), (textLength - 1).coerceAtMost(textRange.endOffset))
    }

}