package com.github.gtache.lsp.editor.listeners

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener

/**
 * Class listening for mouse movement in an editor (used for hover)
 */
class EditorMouseMotionListenerImpl : EditorMouseMotionListener, AbstractLSPListener() {

    override fun mouseDragged(e: EditorMouseEvent): Unit {}

    override fun mouseMoved(e: EditorMouseEvent): Unit {
        if (checkEnabled()) {
            manager?.mouseMoved(e)
        }
    }
}