package com.github.gtache.lsp.settings.application

import com.github.gtache.lsp.requests.Timeout
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger

/**
 * Class managing the persistent state of the LSP application settings
 */
@State(name = "LSPApplicationState", storages = [Storage(value = "LSPApplicationState.xml")])
class LSPPersistentApplicationSettingsImpl : LSPPersistentApplicationSettings {

    override var appState: LSPApplicationState = LSPApplicationState()

    override fun equals(other: Any?): Boolean {
        return other is LSPPersistentApplicationSettingsImpl && appState == other.appState
    }

    override fun loadState(lspState: LSPApplicationState) {
        appState = lspState
        logger.info("LSP Application State loaded")
        if (appState.timeouts.isNotEmpty()) {
            Timeout.timeouts = appState.timeouts
        }
    }

    companion object {
        private val logger = Logger.getInstance(LSPPersistentApplicationSettingsImpl::class.java)
    }

    override fun getState(): LSPApplicationState = appState

    override fun hashCode(): Int {
        return appState.hashCode()
    }
}