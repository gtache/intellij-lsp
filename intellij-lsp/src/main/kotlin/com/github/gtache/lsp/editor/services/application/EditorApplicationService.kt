package com.github.gtache.lsp.editor.services.application

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor

/**
 * Represents a service helping to manage editors for the whole application
 */
interface EditorApplicationService {

    /**
     * Returns the manager for the given [editor]
     */
    fun managerForEditor(editor: Editor): EditorEventManager?

    /**
     * Returns the manager for the given [document]
     */
    fun managerForDocument(document: Document): EditorEventManager?

    /**
     * Returns the editor for the given [document]
     */
    fun editorForDocument(document: Document): Editor?

    /**
     * Notifies that an [editor] has been opened
     */
    fun editorOpened(editor: Editor): Unit

    /**
     * Notifies that an [editor] has been closed
     */
    fun editorClosed(editor: Editor): Unit

    /**
     * Notifies that all the documents will be saved
     */
    fun willSaveAll(): Unit

    /**
     * Adds a [manager]
     */
    fun addManager(manager: EditorEventManager): Unit

    /**
     * Removes a [manager]
     */
    fun removeManager(manager: EditorEventManager): Unit

}