package com.github.gtache.lsp.settings.project

import com.intellij.openapi.components.PersistentStateComponent

/**
 * Represents LSP project settings
 */
interface LSPProjectSettings : PersistentStateComponent<LSPProjectState> {
    /**
     * The current project settings state
     */
    var projectState: LSPProjectState
}