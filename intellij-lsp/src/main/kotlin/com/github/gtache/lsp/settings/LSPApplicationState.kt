package com.github.gtache.lsp.settings

import com.github.gtache.lsp.requests.Timeouts

data class LSPApplicationState(var timeouts: Map<Timeouts, Long>, var additionalRepositories: List<String>) {
    constructor() : this(emptyMap(), emptyList())

    fun withTimeouts(timeouts: Map<Timeouts, Long>): LSPApplicationState {
        return LSPApplicationState(timeouts, additionalRepositories)
    }

    fun withAdditionalRepositories(additionalRepositories: List<String>): LSPApplicationState {
        return LSPApplicationState(timeouts, additionalRepositories)
    }

}