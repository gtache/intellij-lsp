package com.github.gtache.lsp.settings.application

import com.intellij.openapi.components.PersistentStateComponent

interface LSPApplicationSettings : PersistentStateComponent<LSPApplicationState> {
    var appState: LSPApplicationState
}