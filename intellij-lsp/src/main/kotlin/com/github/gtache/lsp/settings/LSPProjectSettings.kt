package com.github.gtache.lsp.settings

import com.intellij.openapi.components.PersistentStateComponent

interface LSPProjectSettings : PersistentStateComponent<LSPProjectState> {
    var projectState : LSPProjectState
}