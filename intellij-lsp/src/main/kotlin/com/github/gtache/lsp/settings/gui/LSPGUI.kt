package com.github.gtache.lsp.settings.gui

import com.github.gtache.lsp.settings.LSPState
import javax.swing.JPanel

interface LSPGUI {

    companion object {
        val lspState: LSPState? = LSPState.instance
    }

    fun state(): LSPState? = lspState

    fun isModified(): Boolean

    fun reset(): Unit

    fun apply(): Unit

    fun getRootPanel(): JPanel

}