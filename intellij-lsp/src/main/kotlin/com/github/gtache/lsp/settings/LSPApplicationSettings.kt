package com.github.gtache.lsp.settings

import com.intellij.openapi.components.PersistentStateComponent

interface LSPApplicationSettings : PersistentStateComponent<LSPApplicationState> {
    var appState: LSPApplicationState
}