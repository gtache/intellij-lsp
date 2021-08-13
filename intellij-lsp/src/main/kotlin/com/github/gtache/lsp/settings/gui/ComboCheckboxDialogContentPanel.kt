package com.github.gtache.lsp.settings.gui

import com.intellij.openapi.ui.ComboBox
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.uiDesigner.core.Spacer
import com.intellij.util.ui.JBUI
import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JPanel

class ComboCheckboxDialogContentPanel(serverDefinitions: List<String>, serverWrappers: List<String>) {
    private val serverDefModel: ComboBoxModel<String>
    private val serverWrapModel: ComboBoxModel<String>
    val rootPane: JPanel = JPanel()
    private var serverBox: JComboBox<String>

    val comboBoxIndex: Int
        get() = serverBox.selectedIndex

    private fun setupUI() {
        rootPane.layout = GridLayoutManager(4, 3, JBUI.emptyInsets(), -1, -1)
        rootPane.add(
            serverBox,
            GridConstraints(
                1,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        )
        val spacer1 = Spacer()
        rootPane.add(
            spacer1,
            GridConstraints(
                3,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_VERTICAL,
                1,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        )
        val spacer2 = Spacer()
        rootPane.add(
            spacer2,
            GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_VERTICAL,
                1,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        )
        val spacer3 = Spacer()
        rootPane.add(
            spacer3,
            GridConstraints(
                2,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_VERTICAL,
                1,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        )
        val spacer4 = Spacer()
        rootPane.add(
            spacer4,
            GridConstraints(
                1,
                2,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                1,
                null,
                null,
                null,
                0,
                false
            )
        )
        val spacer5 = Spacer()
        rootPane.add(
            spacer5,
            GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                1,
                null,
                null,
                null,
                0,
                false
            )
        )
    }

    init {
        serverDefModel = DefaultComboBoxModel(serverDefinitions.toTypedArray())
        serverWrapModel = DefaultComboBoxModel(serverWrappers.toTypedArray())
        serverBox = ComboBox(serverDefModel)
        setupUI()
    }
}