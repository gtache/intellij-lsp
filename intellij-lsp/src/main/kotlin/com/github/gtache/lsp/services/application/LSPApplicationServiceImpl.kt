package com.github.gtache.lsp.services.application

import com.github.gtache.lsp.settings.application.LSPApplicationSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger

/**
 * Implementation of LSPApplicationService
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