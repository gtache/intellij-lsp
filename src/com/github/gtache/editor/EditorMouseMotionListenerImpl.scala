package com.github.gtache.editor

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.{EditorMouseEvent, EditorMouseMotionListener}

/**
  * Class used for the hover event
  */
class EditorMouseMotionListenerImpl extends EditorMouseMotionListener {

  private var manager: EditorEventManager = _
  private val LOG: Logger = Logger.getInstance(classOf[EditorMouseMotionListenerImpl])

  def setManager(manager: EditorEventManager): Unit = {
    this.manager = manager
  }

  override def mouseDragged(e: EditorMouseEvent): Unit = {}

  override def mouseMoved(e: EditorMouseEvent): Unit = {
    if (manager == null) {
      throw new IllegalStateException("No manager")
    } else {
      manager.mouseMoved(e)
    }
  }
}
