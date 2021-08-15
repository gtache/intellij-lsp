package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.editor.EditorApplicationService
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentSynchronizationVetoer

/**
 * This class is used to reject save requests
 * It is used for willSaveWaitUntil to allow time to apply the edits
 */
//TODO check called before willSave
class LSPFileDocumentSynchronizationVetoer : FileDocumentSynchronizationVetoer() {
    override fun maySaveDocument(document: Document, isSaveExplicit: Boolean): Boolean {
        val manager = service<EditorApplicationService>().forDocument(document)
        return if (manager != null) {
            if (manager.needSave) {
                val ret = super.maySaveDocument(document, isSaveExplicit)
                manager.needSave = !ret
                ret
            } else if (manager.wrapper.isWillSaveWaitUntil()) false
            else super.maySaveDocument(document, isSaveExplicit)
        } else {
            super.maySaveDocument(document, isSaveExplicit)
        }
    }
}