package com.github.gtache.lsp

import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator

class LSPStartupActivity : PreloadingActivity() {
    override fun preload(indicator: ProgressIndicator) {
        service<LSPApplicationService>() //Instantiate application service to load settings, etc
    }
}