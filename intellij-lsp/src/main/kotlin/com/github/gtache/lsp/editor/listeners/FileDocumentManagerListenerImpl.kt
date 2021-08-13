package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.requests.FileEventManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.vfs.VirtualFile

/**
 * A FileDocumentManagerListener implementation which listens to beforeDocumentSaving / beforeAllDocumentsSaving
 */
object FileDocumentManagerListenerImpl : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document): Unit {
        FileEventManager.willSave(document)
    }

    override fun unsavedDocumentsDropped(): Unit {}

    override fun beforeAllDocumentsSaving(): Unit = FileEventManager.willSaveAllDocuments()

    override fun beforeFileContentReload(virtualFile: VirtualFile, document: Document): Unit {}

    override fun fileWithNoDocumentChanged(virtualFile: VirtualFile): Unit {}

    override fun fileContentReloaded(virtualFile: VirtualFile, document: Document): Unit {}

    override fun fileContentLoaded(virtualFile: VirtualFile, document: Document): Unit {}
}