package com.github.gtache.lsp.settings

import com.github.gtache.lsp.requests.Timeouts

interface LSPApplicationState {
    var timeouts: Map<Timeouts, Long>
    var additionalRepositories: List<String>
}