package com.github.gtache.editor

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.{EditorMouseEvent, EditorMouseMotionListener}

/**
  * Class listening for mouse movement in an editor (used for hover)
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
      LOG.error("No manager")
    } else {
      manager.mouseMoved(e)
    }
  }
}
