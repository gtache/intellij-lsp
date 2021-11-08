package com.github.gtache.lsp.editor.services.project

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.editor.Editor

/**
 * Represents a service helping to manage editors per project
 */
interface EditorProjectService {
    /**
     * Returns the manager for the given [uri]
     */
    fun forUri(uri: String): EditorEventManager?

    /**
     * Returns all the editors in this project
     */
    fun getEditors(): Set<Editor>

    /**
     * Adds a [manager] to the list of managers
     */
    fun addManager(manager: EditorEventManager): Unit

    /**
     * Removes a [manager] from the list of managers
     */
    fun removeManager(manager: EditorEventManager): Unit
}