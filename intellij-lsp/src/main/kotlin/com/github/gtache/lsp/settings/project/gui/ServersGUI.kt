package com.github.gtache.lsp.settings.project.gui

import com.github.gtache.lsp.client.languageserver.serverdefinition.*
import com.github.gtache.lsp.services.project.LSPProjectService
import com.github.gtache.lsp.settings.gui.LSPGUI
import com.github.gtache.lsp.settings.project.LSPProjectSettings
import com.github.gtache.lsp.utils.Utils
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.uiDesigner.core.Spacer
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ItemEvent
import java.util.function.Consumer
import javax.swing.*

/**
 * The GUI for the LSP ServerDefinition settings
 */
//TODO improve
//TODO add checkbox "LOG messages to/from this server"
class ServersGUI(private val project: Project) : LSPGUI {
    private val rootPanel: JPanel = JPanel()
    private val rows: MutableList<ServersGUIRow> = ArrayList(5)
    private val serverDefinitions: MutableMap<String, UserConfigurableServerDefinition> = LinkedHashMap(5)
    private val projectSettings = project.service<LSPProjectSettings>()
    override fun getRootPanel(): JPanel {
        return rootPanel
    }

    fun getRows(): Collection<ServersGUIRow> {
        return rows
    }

    fun clear() {
        rows.clear()
        serverDefinitions.clear()
        rootPanel.removeAll()
    }

    fun addServerDefinition(serverDefinition: UserConfigurableServerDefinition?) {
        if (serverDefinition != null) {
            serverDefinitions[serverDefinition.ext] = serverDefinition
            when (serverDefinition.javaClass) {
                ArtifactLanguageServerDefinition::class.java -> {
                    val def = serverDefinition as ArtifactLanguageServerDefinition
                    rootPanel.add(createArtifactRow(def.ext, def.packge, def.mainClass, def.args.joinToString(" ")))
                }
                ExeLanguageServerDefinition::class.java -> {
                    val def = serverDefinition as ExeLanguageServerDefinition
                    rootPanel.add(createExeRow(def.ext, def.path, def.args.joinToString(" ")))
                }
                RawCommandServerDefinition::class.java -> {
                    val def = serverDefinition as RawCommandServerDefinition
                    rootPanel.add(createCommandRow(def.ext, def.command.joinToString(" ")))
                }
                else -> {
                    logger.error("Unknown UserConfigurableServerDefinition : $serverDefinition")
                }
            }
        }
    }

    override fun apply() {
        val extensions = rows.map { row ->
            row.getText(EXT)
        }.toMutableList()
        val distinct = extensions.distinct().toSet()
        distinct.forEach(Consumer { o -> extensions.remove(o) })
        if (extensions.isNotEmpty()) {
            Messages.showWarningDialog(extensions.reduce { f, s -> "Duplicate : " + f + Utils.lineSeparator + s }, "Duplicate Extensions")
        }
        serverDefinitions.clear()
        for (row in rows) {
            val arr = row.toStringArray()
            val ext = row.getText(EXT)
            val serverDefinition: UserConfigurableServerDefinition? = UserConfigurableServerDefinition.fromArray(arr)
            if (serverDefinition != null) {
                serverDefinitions[ext] = serverDefinition
            }
        }
        projectSettings.projectState = projectSettings.projectState.withExtToServ(serverDefinitions.mapValues { it.value.toArray() })
        project.service<LSPProjectService>().extToServerDefinition = serverDefinitions
    }

    override fun isModified(): Boolean {
        return if (serverDefinitions.size == rows.filter { row ->
                row.toStringArray().drop(1).any { s -> s.isNotEmpty() }
            }.size) {
            for (row in rows) {
                val stateDef = serverDefinitions[row.getText(EXT)]
                val rowDef: UserConfigurableServerDefinition? = UserConfigurableServerDefinition.fromArray(row.toStringArray())
                if (rowDef != null && rowDef != stateDef) {
                    return true
                }
            }
            false
        } else {
            true
        }
    }

    override fun reset() {
        this.clear()
        val state = project.service<LSPProjectSettings>().projectState
        if (state.extToServ.isNotEmpty()) {
            for (serverDefinition in state.extToServ.values) {
                addServerDefinition(UserConfigurableServerDefinition.fromArray(serverDefinition))
            }
        } else {
            rootPanel.add(createArtifactRow("", "", "", ""))
        }
    }

    private fun createComboBox(panel: JPanel, selected: String): JComboBox<String> {
        val typeBox: JComboBox<String> = ComboBox()
        val types = ConfigurableTypes.values()
        for (type in types) {
            typeBox.addItem(type.typ)
        }
        typeBox.selectedItem = selected
        typeBox.addItemListener { e: ItemEvent ->
            if (e.stateChange == ItemEvent.SELECTED) {
                val idx = getComponentIndex(panel)
                when (e.item) {
                    ConfigurableTypes.ARTIFACT.typ -> {
                        rootPanel.add(createArtifactRow("", "", "", ""), idx)
                        rootPanel.remove(panel)
                        rows.removeAt(idx)
                    }
                    ConfigurableTypes.RAWCOMMAND.typ -> {
                        rootPanel.add(createCommandRow("", ""), idx)
                        rootPanel.remove(panel)
                        rows.removeAt(idx)
                    }
                    ConfigurableTypes.EXE.typ -> {
                        rootPanel.add(createExeRow("", "", ""), idx)
                        rootPanel.remove(panel)
                        rows.removeAt(idx)
                    }
                    else -> {
                        logger.error("Unknown type : " + e.item)
                    }
                }
            }
        }
        return typeBox
    }

    private fun createNewRowButton(): JButton {
        val newRowButton = JButton()
        newRowButton.text = "+"
        newRowButton.addActionListener { rootPanel.add(createArtifactRow("", "", "", "")) }
        return newRowButton
    }

    private fun createRemoveRowButton(panel: JPanel): JButton {
        val removeRowButton = JButton()
        removeRowButton.text = "-"
        removeRowButton.addActionListener {
            val idx = getComponentIndex(panel)
            rootPanel.remove(panel)
            rows.removeAt(idx)
        }
        return removeRowButton
    }

    private fun createRow(labelFields: Collection<JComponent>, selectedItem: String): JPanel {
        val panel: JBPanel<*> = JBPanel<JBPanel<*>>(GridLayoutManager(2, 17, JBUI.emptyInsets(), -1, -1))
        var colIdx = 0
        panel.add(
            Spacer(),
            GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_NONE,
                1,
                GridConstraints.SIZEPOLICY_FIXED,
                Dimension(0, 10),
                Dimension(0, 10),
                Dimension(0, 10),
                0,
                false
            )
        )
        val typeBox = createComboBox(panel, selectedItem)
        panel.add(
            typeBox,
            GridConstraints(
                0,
                colIdx++,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        )
        val iterator = labelFields.iterator()
        while (iterator.hasNext()) {
            val label = iterator.next()
            val field = iterator.next()
            panel.add(
                Spacer(),
                GridConstraints(
                    0,
                    colIdx++,
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
            )
            panel.add(
                label,
                GridConstraints(
                    0,
                    colIdx++,
                    1,
                    1,
                    GridConstraints.ANCHOR_WEST,
                    GridConstraints.FILL_NONE,
                    GridConstraints.SIZEPOLICY_FIXED,
                    GridConstraints.SIZEPOLICY_FIXED,
                    null,
                    null,
                    null,
                    0,
                    false
                )
            )
            panel.add(
                field,
                GridConstraints(
                    0,
                    colIdx++,
                    1,
                    1,
                    GridConstraints.ANCHOR_WEST,
                    GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_FIXED,
                    null,
                    Dimension(150, -1),
                    null,
                    0,
                    false
                )
            )
        }
        panel.add(
            Spacer(),
            GridConstraints(
                0,
                14,
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
        val newRowButton = createNewRowButton()
        panel.add(
            newRowButton,
            GridConstraints(
                0,
                15,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        )
        if (rows.isEmpty()) {
            panel.add(
                Spacer(),
                GridConstraints(
                    0,
                    16,
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
            )
        } else {
            val removeRowButton = createRemoveRowButton(panel)
            panel.add(
                removeRowButton,
                GridConstraints(
                    0,
                    16,
                    1,
                    1,
                    GridConstraints.ANCHOR_CENTER,
                    GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_FIXED,
                    null,
                    null,
                    null,
                    0,
                    false
                )
            )
        }
        panel.alignmentX = Component.LEFT_ALIGNMENT
        return panel
    }

    private fun createArtifactRow(ext: String, serv: String, mainClass: String, args: String): JPanel {
        val extLabel: JLabel = JBLabel(EXT_LABEL)
        val extField: JTextField = JBTextField()
        extField.toolTipText = EXT_TOOLTIP
        extField.text = ext
        val packgeLabel: JLabel = JBLabel("Artifact")
        val packgeField = JTextArea()
        packgeField.lineWrap = true
        packgeField.toolTipText = "e.g. ch.epfl.lamp:dotty-language-server_0.3:0.3.0-RC2"
        packgeField.text = serv
        val mainClassLabel: JLabel = JBLabel("Main class")
        val mainClassField = JTextArea()
        mainClassField.lineWrap = true
        mainClassField.toolTipText = "e.g. dotty.tools.languageserver.Main"
        mainClassField.text = mainClass
        val argsLabel: JLabel = JBLabel("Args")
        val argsField = JTextArea()
        argsField.lineWrap = true
        argsField.toolTipText = "e.g. -stdio"
        argsField.text = args
        val components = listOf<JComponent>(extLabel, extField, packgeLabel, packgeField, mainClassLabel, mainClassField, argsLabel, argsField)
        val panel = createRow(components, ArtifactLanguageServerDefinition.presentableTyp)
        val map = LinkedHashMap<String, JComponent>()
        map.put(EXT, extField)
        map.put(PACKGE, packgeField)
        map.put(MAINCLASS, mainClassField)
        map.put(ARGS, argsField)
        rows.add(ServersGUIRow(panel, ArtifactLanguageServerDefinition.typ, map))
        return panel
    }

    private fun createExeRow(ext: String, path: String, args: String): JPanel {
        val extLabel: JLabel = JBLabel(EXT_LABEL)
        val extField: JTextField = JBTextField()
        extField.toolTipText = EXT_TOOLTIP
        extField.text = ext
        val pathLabel: JLabel = JBLabel(FILE_PATH_LABEL)
        val pathField = TextFieldWithBrowseButton()
        pathField.toolTipText = "e.g. C:\\rustLS\\rls.exe"
        pathField.text = path
        pathField.addBrowseFolderListener(TextBrowseFolderListener(FileChooserDescriptor(true, false, true, true, true, false).withShowHiddenFiles(true)))
        val argsLabel: JLabel = JBLabel("Args")
        val argsField = JTextArea()
        argsField.lineWrap = true
        argsField.toolTipText = "e.g. -stdio"
        argsField.text = args
        val components = listOf<JComponent>(extLabel, extField, pathLabel, pathField, argsLabel, argsField)
        val panel = createRow(components, ExeLanguageServerDefinition.presentableTyp)
        val map = LinkedHashMap<String, JComponent>()
        map.put(EXT, extField)
        map.put(PATH, pathField)
        map.put(ARGS, argsField)
        rows.add(ServersGUIRow(panel, ExeLanguageServerDefinition.typ, map))
        return panel
    }

    private fun createCommandRow(ext: String, command: String): JPanel {
        val extLabel: JLabel = JBLabel(EXT_LABEL)
        val extField: JTextField = JBTextField()
        extField.toolTipText = EXT_TOOLTIP
        extField.text = ext
        val commandLabel: JLabel = JBLabel("Command")
        val argsField = JTextArea()
        argsField.lineWrap = true
        argsField.text = command
        argsField.toolTipText = "e.g. python.exe -m C:\\python-ls\\pyls"
        val components = listOf<JComponent>(extLabel, extField, commandLabel, argsField)
        val panel = createRow(components, RawCommandServerDefinition.presentableTyp)
        val map = LinkedHashMap<String, JComponent>()
        map.put(EXT, extField)
        map.put(COMMAND, argsField)
        rows.add(ServersGUIRow(panel, RawCommandServerDefinition.typ, map))
        return panel
    }

    private fun getComponentIndex(component: JComponent): Int {
        for (i in 0 until rootPanel.componentCount) {
            if (rootPanel.getComponent(i) == component) {
                return i
            }
        }
        return -1
    }

    companion object {
        private const val EXT_LABEL = "Extension"
        private const val EXT_TOOLTIP = "e.g. scala, java, c, js, ..."
        private const val EXT = "ext"
        private const val MAINCLASS = "mainclass"
        private const val ARGS = "args"
        private const val PACKGE = "packge"
        private const val COMMAND = "command"
        private const val PATH = "path"
        private val logger = Logger.getInstance(ServersGUI::class.java)
        private const val FILE_PATH_LABEL = "Path"
    }

    init {
        rootPanel.layout = BoxLayout(rootPanel, BoxLayout.Y_AXIS)
        rootPanel.add(createArtifactRow("", "", "", ""))
    }
}