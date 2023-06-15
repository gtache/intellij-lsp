package com.github.gtache.lsp.settings.project.gui

import com.github.gtache.lsp.languageserver.definition.DefinitionKey
import com.github.gtache.lsp.utils.CSVLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.JTextField

/**
 * Class representing a row in the server settings window
 *
 * @param id The id of the definition
 * @param type The type of the definition
 */
data class ServersGUIRow(val id: String, val type: String, private val project: Project, private val fields: Map<DefinitionKey, JComponent>) {
    companion object {
        private val logger: Logger = Logger.getInstance(ServersGUIRow::class.java)
    }

    /**
     * Returns the text value for the given field [key]
     */
    fun getText(key: DefinitionKey): String {
        return when (val component = fields[key]) {
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
    fun toCSVLines(): List<CSVLine> {
        return listOf(CSVLine.of(type), CSVLine.of(id)) + fields.values.map {
            when (it) {
                is JTextField -> CSVLine.of(it.text.trim())
                is JTextArea -> CSVLine.of(it.text.trim())
                is TextFieldWithBrowseButton -> CSVLine.of(it.text.trim())
                is JComboBox<*> -> CSVLine.of(it.selectedItem as String)
                else -> {
                    logger.error("Unknown JComponent : $it")
                    throw IllegalStateException("Should not happen $it")
                }
            }
        }
    }

    fun openAdvancedSettings(): AdvancedServerSettingsGUI {
        val gui = AdvancedServerSettingsGUI(project, id)
        val builder = DialogBuilder(project)
        builder.setCenterPanel(gui.getRootPanel())
        builder.setTitle("Advanced settings")

        if (!builder.showAndGet()) {
            gui.reset()
        }
        return gui
    }
}