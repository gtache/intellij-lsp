package com.github.gtache.editor.listeners

import com.intellij.openapi.editor.event.{EditorMouseEvent, EditorMouseListener}

/**
  * An EditorMouseListener implementation which just listens to mouseExited and mouseEntered
  */
class EditorMouseListenerImpl extends EditorMouseListener with LSPListener {

  override def mouseExited(e: EditorMouseEvent): Unit = {
    if (checkManager()) manager.stopListening()
  }

  override def mousePressed(e: EditorMouseEvent): Unit = {
  }

  override def mouseReleased(e: EditorMouseEvent): Unit = {
  }

  override def mouseEntered(e: EditorMouseEvent): Unit = {
    if (checkManager()) manager.startListening()
  }

  override def mouseClicked(e: EditorMouseEvent): Unit = {
  }
}
