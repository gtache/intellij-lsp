package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentSynchronizationVetoer

/**
 * This class is used to reject save requests
 * It is used for willSaveWaitUntil to allow time to apply the edits
 */
//TODO check called before willSave
class LSPFileDocumentSynchronizationVetoer : FileDocumentSynchronizationVetoer() {
    override fun maySaveDocument(document: Document, isSaveExplicit: Boolean): Boolean {
        val manager = FileUtils.documentToUri(document)?.let { EditorEventManager.forUri(it) }
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