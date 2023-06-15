package com.github.gtache.lsp.languageserver.settings

data class SettingsImpl(
    override val isLogging: Boolean,
    override val logDir: String,
    override val isAlwaysSendRequests: Boolean
) : Settings