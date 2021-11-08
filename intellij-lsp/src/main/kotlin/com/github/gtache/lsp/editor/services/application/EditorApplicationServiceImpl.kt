package com.github.gtache.lsp.editor.services.application

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor

/**
 * Implementation of EditorApplicationService
 */
class EditorApplicationServiceImpl : EditorApplicationService {

    private val editorToManager: MutableMap<Editor, EditorEventManager> = HashMap()
    private val documentToEditor: MutableMap<Document, Editor> = HashMap()


    override fun managerForEditor(editor: Editor): EditorEventManager? {
        prune()
        return editorToManager[editor]
    }

    override fun managerForDocument(document: Document): EditorEventManager? {
        return editorToManager[editorForDocument(document)]
    }

    override fun editorForDocument(document: Document): Editor? {
        return documentToEditor[document]
    }

    override fun editorOpened(editor: Editor) {
        documentToEditor[editor.document] = editor
    }

    override fun editorClosed(editor: Editor) {
        documentToEditor -= editor.document
    }

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