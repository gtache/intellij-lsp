package com.github.gtache.lsp.settings.gui

import javax.swing.JPanel

interface LSPGUI {

    fun isModified(): Boolean

    fun reset(): Unit

    fun apply(): Unit

    fun getRootPanel(): JPanel

}