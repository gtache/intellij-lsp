package com.github.gtache.lsp.utils

import java.util.*

/**
 * Object containing some useful methods for the plugin
 */
object Utils {

    /**
     * The resource bundle used to display localized strings
     */
    @JvmStatic
    val BUNDLE: ResourceBundle = ResourceBundle.getBundle("com.github.gtache.lsp.LSPBundle")

    /**
     * The system line separator
     */
    @JvmStatic
    val LINE_SEPARATOR: String = System.getProperty("line.separator")
}