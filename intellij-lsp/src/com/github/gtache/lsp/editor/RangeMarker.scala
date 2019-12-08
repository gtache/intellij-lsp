package com.github.gtache.lsp.editor

import java.awt.Cursor

import com.github.gtache.lsp.utils.DocumentUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import org.eclipse.lsp4j.{Location, LocationLink}

/**
  * Class used to store a specific range corresponding to the element under the mouse
  *
  * @param loc    The location of the definition of the element under the mouse
  * @param editor The current editor
  * @param range  The range of the element under the mouse
  */
case class RangeMarker(startOffset: Int, endOffset: Int, editor: Editor, loc: LocationLink = null, range: RangeHighlighter = null, isDefinition: Boolean = true) {

  def highlightContainsOffset(offset: Int): Boolean = {
    if (!isDefinition) startOffset <= offset && endOffset >= offset else definitionContainsOffset(offset)
  }

  def definitionContainsOffset(offset: Int): Boolean = {
    if (loc != null) DocumentUtils.LSPPosToOffset(editor, loc.getTargetRange.getStart) <= offset && offset <= DocumentUtils.LSPPosToOffset(editor, loc.getTargetRange.getEnd) else false
  }

  /**
    * Removes the highlighter and restores the default cursor
    */
  def dispose(): Unit = {
    if (range != null) {
      editor.getMarkupModel.removeHighlighter(range)
      editor.getContentComponent.setCursor(Cursor.getDefaultCursor)
    }
  }
}
