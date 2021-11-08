package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.diagnostic.Logger

/**
 * Interface for all the LSP listeners depending on a manager
 */
interface LSPListener {
    companion object {
        private val logger: Logger = Logger.getInstance(LSPListener::class.java)
    }

    /**
     * The manager for this listener
     */
    var manager: EditorEventManager?

    /**
     * Whether the listener is enabled or not
     */
    var enabled: Boolean

    /**
     * Checks if the listener must currently report on events
     */
    fun checkEnabled(): Boolean {
        return if (manager == null) {
            logger.error("Manager is null")
            false
        } else enabled
    }

}