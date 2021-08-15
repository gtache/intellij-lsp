package com.github.gtache.lsp.editor

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor

class EditorApplicationServiceImpl : EditorApplicationService {
    companion object {
        private val logger: Logger = Logger.getInstance(EditorApplicationServiceImpl::class.java)
    }

    private val editorToManager: MutableMap<Editor, EditorEventManager> = HashMap()
    private val documentToEditor: MutableMap<Document, Editor> = HashMap()


    override fun forEditor(editor: Editor): EditorEventManager? {
        prune()
        return editorToManager[editor]
    }

    override fun forDocument(document: Document): EditorEventManager? {
        return editorToManager[getEditor(document)]
    }

    override fun getEditor(document: Document): Editor? {
        return documentToEditor[document]
    }

    override fun editorOpened(editor: Editor) {
        documentToEditor[editor.document] = editor
    }

    override fun editorClosed(editor: Editor) {
        documentToEditor -= editor.document
    }

    fun getEditors(): Set<Editor> {
        return editorToManager.keys
    }

    /**
     * Tells all the servers that all the documents will be saved
     */
    override fun willSaveAll(): Unit {
        prune()
        editorToManager.forEach { e -> e.value.willSave() }
    }

    override fun addManager(manager: EditorEventManager) {
        editorToManager[manager.editor] = manager
    }

    override fun removeManager(manager: EditorEventManager) {
        editorToManager -= manager.editor
    }

    private fun prune(): Unit {
        editorToManager.filter { e -> !e.value.wrapper.isActive() }.keys.forEach { editorToManager.remove(it) }
    }
}