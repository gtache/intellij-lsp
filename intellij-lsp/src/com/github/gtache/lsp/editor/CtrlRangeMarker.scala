package com.github.gtache.lsp.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.psi.impl.source.PsiFieldImpl
import org.eclipse.lsp4j.Location

/**
  * Class used to store a specific range corresponding to the element under the mouse when Ctrl is pressed
  *
  * @param loc    The location of the definition of the element under the mouse
  * @param editor The current editor
  * @param range  The range of the element under the mouse (represented as an hyperlink)
  */
case class CtrlRangeMarker(loc: Location, editor: Editor, range: RangeHighlighter) {

  def containsOffset(offset: Int): Boolean = {
    range.getStartOffset <= offset && range.getEndOffset >= offset
  }

  def dispose(): Unit = {
    editor.getMarkupModel.removeHighlighter(range)
  }

}
