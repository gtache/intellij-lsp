package com.github.gtache.lsp.editor.listeners

import com.intellij.openapi.editor.event.{SelectionEvent, SelectionListener}

/**
  * Implementation of a SelectionListener
  */
class SelectionListenerImpl extends SelectionListener with LSPListener {

  override def selectionChanged(e: SelectionEvent): Unit =
    if (checkEnabled()) manager.selectionChanged(e)
}
