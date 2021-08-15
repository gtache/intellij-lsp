package com.github.gtache.lsp.editor

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor

interface EditorApplicationService {

    /**
     * @param editor An editor
     * @return The manager for the given editor, or None
     */
    fun forEditor(editor: Editor): EditorEventManager?

    fun forDocument(document: Document): EditorEventManager?

    fun getEditor(document: Document): Editor?

    fun editorOpened(editor: Editor): Unit

    fun editorClosed(editor: Editor): Unit

    /**
     * Tells all the servers that all the documents will be saved
     */
    fun willSaveAll(): Unit

    fun addManager(manager: EditorEventManager): Unit

    fun removeManager(manager: EditorEventManager): Unit

}