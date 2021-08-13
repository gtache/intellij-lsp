package com.github.gtache.lsp.editor

import com.github.gtache.lsp.utils.DocumentUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import org.eclipse.lsp4j.LocationLink
import java.awt.Cursor

/**
 * Class used to store a specific range corresponding to the element under the mouse
 *
 * @param loc    The location of the definition of the element under the mouse
 * @param editor The current editor
 * @param range  The range of the element under the mouse
 */
data class RangeMarker(
    val startOffset: Int,
    val endOffset: Int,
    private val editor: Editor,
    val loc: LocationLink? = null,
    private val range: RangeHighlighter? = null,
    private val isDefinition: Boolean = true
) {

    fun highlightContainsOffset(offset: Int): Boolean {
        return if (!isDefinition) offset in startOffset..endOffset else definitionContainsOffset(offset)
    }

    fun definitionContainsOffset(offset: Int): Boolean {
        return if (loc != null) DocumentUtils.LSPPosToOffset(editor, loc.targetRange.start) <= offset && offset <= DocumentUtils.LSPPosToOffset(
            editor,
            loc.targetRange.end
        ) else false
    }

    /**
     * Removes the highlighter and restores the default cursor
     */
    fun dispose(): Unit {
        range?.let {
            editor.markupModel.removeHighlighter(it)
            editor.contentComponent.cursor = Cursor.getDefaultCursor()
        }
    }
}