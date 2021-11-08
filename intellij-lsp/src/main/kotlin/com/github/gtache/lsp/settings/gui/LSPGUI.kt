package com.github.gtache.lsp.settings.gui

import javax.swing.JPanel

/**
 * Represents a settings GUI
 */
interface LSPGUI {

    /**
     * Whether the values are modified or not
     */
    fun isModified(): Boolean

    /**
     * Resets to the initial values
     */
    fun reset(): Unit

    /**
     * Applies the selected values
     */
    fun apply(): Unit

    /**
     * Returns the root panel
     */
    fun getRootPanel(): JPanel

}