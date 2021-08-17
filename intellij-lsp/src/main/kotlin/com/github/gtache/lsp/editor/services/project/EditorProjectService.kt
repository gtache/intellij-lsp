package com.github.gtache.lsp.editor.services.project

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.editor.Editor

interface EditorProjectService {
    /**
     * @param uri A file uri
     * @return The manager for the given uri, or None
     */
    fun forUri(uri: String): EditorEventManager?

    /**
     * @return All the editors in this project
     */
    fun getEditors(): Set<Editor>

    fun addManager(manager: EditorEventManager): Unit

    fun removeManager(manager: EditorEventManager): Unit
}