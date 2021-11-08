package com.github.gtache.lsp.editor.services.project

import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

/**
 * Implementation of EditorProjectService
 */
class EditorProjectServiceImpl(private val project: Project) : EditorProjectService {

    companion object {
        private val logger: Logger = Logger.getInstance(EditorProjectServiceImpl::class.java)
    }

    private val uriToManager: MutableMap<String, EditorEventManager> = HashMap()

    override fun forUri(uri: String): EditorEventManager? {
        prune()
        return uriToManager[uri]
    }

    override fun getEditors(): Set<Editor> {
        return uriToManager.values.map { it.editor }.toSet()
    }

    override fun addManager(manager: EditorEventManager) {
        val uri = FileUtils.editorToURIString(manager.editor)
        if (uri != null) {
            uriToManager[uri] = manager
        } else {
            logger.warn("Null URI for ${manager.editor}")
        }
    }

    override fun removeManager(manager: EditorEventManager) {
        FileUtils.editorToURIString(manager.editor)?.let { uriToManager -= it }
    }

    private fun prune(): Unit {
        uriToManager.filter { e -> !e.value.wrapper.isActive() }.keys.forEach { uriToManager.remove(it) }
    }
}