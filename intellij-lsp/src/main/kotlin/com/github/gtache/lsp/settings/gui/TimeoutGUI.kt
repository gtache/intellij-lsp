package com.github.gtache.lsp.settings.gui

import com.github.gtache.lsp.requests.Timeout.timeouts
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.settings.LSPState.Companion.instance
import com.intellij.openapi.diagnostic.Logger
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.uiDesigner.core.Spacer
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.text.NumberFormat
import java.util.*
import javax.swing.JFormattedTextField
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.text.NumberFormatter

/**
 * GUI for the Timeouts settings
 */
class TimeoutGUI : LSPGUI {
    private val rows: Map<Timeouts, JTextField>
    private val state = instance
    private val rootPanel: JPanel

    override fun getRootPanel(): JPanel {
        return rootPanel
    }

    private fun createRootPanel(): JPanel {
        val panel = JPanel()
        panel.layout = GridLayoutManager(rows.size, 6, JBUI.emptyInsets(), -1, -1)
        var idx = 0
        val iterator = rows.entries.sortedBy { it.key.name }.iterator()
        while (iterator.hasNext()) {
            var entry = iterator.next()
            var timeout = entry.key
            var textField = entry.value
            var name = timeout.name
            panel.add(JLabel(name.substring(0, 1) + name.substring(1).lowercase(Locale.getDefault())), createGridConstraints(idx, 0))
            panel.add(textField, createGridConstraints(idx, 1, Dimension(100, 10)))
            panel.add(Spacer(), createSpacerGridConstraints(idx, 2))
            if (iterator.hasNext()) {
                entry = iterator.next()
                timeout = entry.key
                textField = entry.value
                name = timeout.name
                panel.add(JLabel(name.substring(0, 1) + name.substring(1).lowercase(Locale.getDefault())), createGridConstraints(idx, 3))
                panel.add(textField, createGridConstraints(idx, 4, Dimension(100, 10)))
                panel.add(Spacer(), createSpacerGridConstraints(idx++, 5))
            }
        }
        return panel
    }

    override fun apply() {
        val newTimeouts = rows.map { e -> Pair(e.key, e.value.text.toLong()) }.toMap()
        state?.timeouts = newTimeouts
        timeouts = newTimeouts
    }

    override fun reset() {
        val currentTimeouts = timeouts
        rows.forEach { (timeout: Timeouts, textField: JTextField) ->
            textField.text = currentTimeouts[timeout].toString()
        }
    }

    override fun isModified(): Boolean {
        val currentTimeouts = timeouts
        return try { //Don't allow apply if the value is not valid
            Timeouts.values().any { t: Timeouts ->
                if (rows.containsKey(t)) {
                    val newValue = rows[t]!!.text.toLong()
                    currentTimeouts[t] != newValue && newValue >= 0
                } else {
                    false
                }
            }
        } catch (ignored: NumberFormatException) {
            false
        }
    }

    companion object {
        private const val FIELD_TOOLTIP = "Time in milliseconds"
        private val logger = Logger.getInstance(TimeoutGUI::class.java)
        private fun createGridConstraints(rowIdx: Int, colIdx: Int, preferredSize: Dimension? = null): GridConstraints {
            return GridConstraints(
                rowIdx,
                colIdx,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                preferredSize,
                null,
                0,
                false
            )
        }

        private fun createSpacerGridConstraints(rowIdx: Int, colIdx: Int): GridConstraints {
            return GridConstraints(
                rowIdx,
                colIdx,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                1,
                null,
                null,
                null,
                0,
                false
            )
        }
    }

    init {
        rows = timeouts.map { e ->
            val format = NumberFormat.getInstance()
            val formatter = NumberFormatter(format)
            format.isGroupingUsed = false
            formatter.valueClass = Int::class.java
            formatter.minimum = 0
            formatter.allowsInvalid = true
            formatter.maximum = Int.MAX_VALUE
            val field = JFormattedTextField(formatter)
            field.toolTipText = FIELD_TOOLTIP
            field.text = e.value.toString()
            Pair(e.key, field)
        }.toMap()
        rootPanel = createRootPanel()
    }
}