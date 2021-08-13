package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.PluginMain
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
        PluginMain.editorClosed(editorFactoryEvent.editor)
    }

    override fun editorCreated(editorFactoryEvent: EditorFactoryEvent): Unit {
        PluginMain.editorOpened(editorFactoryEvent.editor)
    }
}