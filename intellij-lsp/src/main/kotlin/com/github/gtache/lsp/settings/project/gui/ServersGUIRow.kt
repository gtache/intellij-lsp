package com.github.gtache.lsp.settings.project.gui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.JTextField

/**
 * Class representing a row in the server settings window
 *
 * @constructor The [type] of server definition along with its [fields]
 */
data class ServersGUIRow(val type: String, private val fields: Map<String, JComponent>) {
    companion object {
        private val logger: Logger = Logger.getInstance(ServersGUIRow::class.java)
    }

    /**
     * Returns the text value for the given field key/[label]
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
     * Returns a string array representing this row
     */
    fun toStringArray(): Array<String> {
        return arrayOf(type) + fields.values.map {
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