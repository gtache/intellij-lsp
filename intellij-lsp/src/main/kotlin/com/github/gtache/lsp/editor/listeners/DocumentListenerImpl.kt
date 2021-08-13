package com.github.gtache.lsp.editor.listeners

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

/**
 * Implementation of a DocumentListener
 */
class DocumentListenerImpl : DocumentListener, AbstractLSPListener() {

    /**
     * Called before the text of the document is changed.
     *
     * @param event the event containing the information about the change.
     */
    override fun beforeDocumentChange(event: DocumentEvent): Unit {
    }


    /**
     * Called after the text of the document has been changed.
     *
     * @param event the event containing the information about the change.
     */
    override fun documentChanged(event: DocumentEvent): Unit {
        if (checkEnabled()) {
            manager?.documentChanged(event)
        }
    }

}