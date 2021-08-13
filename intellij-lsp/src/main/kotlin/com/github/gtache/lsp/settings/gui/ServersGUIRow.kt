package com.github.gtache.lsp.settings.gui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import javax.swing.*

/**
 * Class representing a row in the server settings window
 *
 * @param panel  The row as a JPanel
 * @param typ    The typ of the row
 * @param fields The fields of the row
 */
data class ServersGUIRow(private val panel: JPanel, val typ: String, private val fields: LinkedHashMap<String, JComponent>) {
    companion object {
        private val logger: Logger = Logger.getInstance(ServersGUIRow::class.java)
    }

    /**
     * @param label The label corresponding to the text field
     * @return The content of the text field
     */
    fun getText(label: String): String {
        return when (val component = fields[label]) {
            is JTextField -> component.text.trim()
            is JTextArea -> component.text.trim()
            is TextFieldWithBrowseButton -> component.text.trim()
            is JComboBox<*> -> component.selectedItem as String
            else -> {
                logger.error("Unknown JComponent : $component")
                ""
            }
        }
    }

    /**
     * @return A string array representing this row
     */
    fun toStringArray(): Array<String> {
        return arrayOf(typ) + fields.values.map {
            when (it) {
                is JTextField -> it.text.trim()
                is JTextArea -> it.text.trim()
                is TextFieldWithBrowseButton -> it.text.trim()
                is JComboBox<*> -> it.selectedItem as String
                else -> {
                    logger.error("Unknown JComponent : $it")
                    ""
                }
            }
        }.toTypedArray()
    }
}