package com.github.gtache.lsp.editor.listeners

import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener

/**
 * Implementation of a SelectionListener
 */
class SelectionListenerImpl : SelectionListener, AbstractLSPListener() {

    override fun selectionChanged(e: SelectionEvent): Unit {
        if (checkEnabled()) {
            manager?.selectionChanged(e)
        }
    }
}