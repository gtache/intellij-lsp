package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.editor.EditorApplicationService
import com.github.gtache.lsp.services.project.LSPProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener

/**
 * An EditorListener implementation
 */
class EditorListener : EditorFactoryListener {

    companion object {
        private val logger: Logger = Logger.getInstance(EditorListener::class.java)
    }

    override fun editorReleased(editorFactoryEvent: EditorFactoryEvent): Unit {
        service<EditorApplicationService>().editorClosed(editorFactoryEvent.editor)
        editorFactoryEvent.editor.project?.service<LSPProjectService>()?.editorClosed(editorFactoryEvent.editor)
    }

    override fun editorCreated(editorFactoryEvent: EditorFactoryEvent): Unit {
        service<EditorApplicationService>().editorOpened(editorFactoryEvent.editor)
        editorFactoryEvent.editor.project?.service<LSPProjectService>()?.editorOpened(editorFactoryEvent.editor)
    }
}