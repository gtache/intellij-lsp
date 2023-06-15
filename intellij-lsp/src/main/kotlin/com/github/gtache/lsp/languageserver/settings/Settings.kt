package com.github.gtache.lsp.languageserver.settings

interface Settings {
    val isLogging: Boolean
    val logDir: String
    val isAlwaysSendRequests: Boolean
}