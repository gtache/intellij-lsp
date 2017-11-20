package com.github.gtache.lsp.utils

import java.awt.Point
import javax.swing.JLabel

import com.intellij.codeInsight.hint.{HintManager, HintManagerImpl}
import com.intellij.openapi.editor.Editor
import com.intellij.ui.{Hint, LightweightHint}

object GUIUtils {

  def createAndShowHint(editor: Editor, string: String, point: Point, constraint: Short = HintManager.ABOVE,
                        flags: Int = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING): Hint = {
    val hint = new LightweightHint(new JLabel(string))
    val p = HintManagerImpl.getHintPosition(hint, editor, editor.xyToLogicalPosition(point), constraint)
    HintManagerImpl.getInstanceImpl.showEditorHint(hint, editor, p, flags, 0, false, HintManagerImpl.createHintHint(editor, p, hint, constraint).setContentActive(false))
    hint
  }
}
