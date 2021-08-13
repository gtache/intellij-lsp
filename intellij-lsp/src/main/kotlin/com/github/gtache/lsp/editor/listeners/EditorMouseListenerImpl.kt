package com.github.gtache.lsp.editor.listeners

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener

/**
 * An EditorMouseListener implementation which just listens to mouseExited and mouseEntered
 */
class EditorMouseListenerImpl : EditorMouseListener, AbstractLSPListener() {

    override fun mouseExited(e: EditorMouseEvent): Unit {
        if (checkEnabled()) manager?.mouseExited()
    }

    override fun mousePressed(e: EditorMouseEvent): Unit {
    }

    override fun mouseReleased(e: EditorMouseEvent): Unit {
    }

    override fun mouseEntered(e: EditorMouseEvent): Unit {
        if (checkEnabled()) manager?.mouseEntered()
    }

    override fun mouseClicked(e: EditorMouseEvent): Unit {
        if (checkEnabled()) manager?.mouseClicked(e)
    }
}