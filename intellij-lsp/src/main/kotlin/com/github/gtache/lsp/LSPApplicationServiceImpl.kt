package com.github.gtache.lsp

import com.github.gtache.lsp.settings.LSPApplicationSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger

/**
 * The main class of the plugin
 */
class LSPApplicationServiceImpl : LSPApplicationService {

    init {
        service<LSPApplicationSettings>()
        logger.info("LSPApplication init finished")
    }

    companion object {
        private val logger: Logger = Logger.getInstance(LSPApplicationServiceImpl::class.java)
    }
}