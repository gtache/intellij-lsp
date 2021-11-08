package com.github.gtache.lsp.settings.application

import com.intellij.openapi.components.PersistentStateComponent

/**
 * Represents LSP settings for the entire application
 */
interface LSPApplicationSettings : PersistentStateComponent<LSPApplicationState> {
    /**
     * The current application settings state
     */
    var appState: LSPApplicationState
}