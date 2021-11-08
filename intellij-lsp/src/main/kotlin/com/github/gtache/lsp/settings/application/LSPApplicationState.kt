package com.github.gtache.lsp.settings.application

import com.github.gtache.lsp.requests.Timeouts

/**
 * Represents the current LSP application settings
 * @param timeouts The timeouts values
 * @param additionalRepositories The additional repositories to resolve artifacts
 */
data class LSPApplicationState(var timeouts: Map<Timeouts, Long>, var additionalRepositories: List<String>) {
    constructor() : this(emptyMap(), emptyList())

    /**
     * Returns a new State with the new [timeouts] values
     */
    fun withTimeouts(timeouts: Map<Timeouts, Long>): LSPApplicationState {
        return LSPApplicationState(timeouts, additionalRepositories)
    }

    /**
     * Returns a new State with the new [additionalRepositories] values
     */
    fun withAdditionalRepositories(additionalRepositories: List<String>): LSPApplicationState {
        return LSPApplicationState(timeouts, additionalRepositories)
    }

}