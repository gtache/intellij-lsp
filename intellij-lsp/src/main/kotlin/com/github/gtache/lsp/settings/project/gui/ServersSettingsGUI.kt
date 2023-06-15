package com.github.gtache.lsp.settings.project.gui

import com.github.gtache.lsp.languageserver.definition.*
import com.github.gtache.lsp.languageserver.definition.provider.LanguageServerDefinitionProvider
import com.github.gtache.lsp.services.project.LSPProjectService
import com.github.gtache.lsp.settings.gui.LSPGUI
import com.github.gtache.lsp.settings.project.LSPPersistentProjectSettings
import com.github.gtache.lsp.utils.Utils
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.popup.PopupComponent
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.uiDesigner.core.Spacer
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ItemEvent
import javax.swing.*

/**
 * The GUI for the LSP ServerDefinition settings
 */
//TODO improve
//TODO add checkbox "LOG messages to/from this server"
class ServersSettingsGUI(private val project: Project) : LSPGUI {
    private val rootPanel: JPanel = JPanel()
    private val rows: MutableList<ServersGUIRow> = ArrayList(5)
    private val serverDefinitions: MutableMap<String, Definition> = LinkedHashMap(5)
    private val projectSettings = project.service<LSPPersistentProjectSettings>()
    private val definitionProvider = service<LanguageServerDefinitionProvider>()

    init {
        rootPanel.layout = BoxLayout(rootPanel, BoxLayout.Y_AXIS)
        rootPanel.add(createArtifactRow("", emptyList(), "", "", ""))
    }

    override fun getRootPanel(): JPanel {
        return rootPanel
    }

    /**
     * Clears the GUI
     */
    fun clear() {
        rows.clear()
        serverDefinitions.clear()
        rootPanel.removeAll()
    }

    /**
     * Adds the given [serverDefinition] to the GUI
     */
    fun addServerDefinition(serverDefinition: Definition?) {
        if (serverDefinition != null) {
            serverDefinitions[serverDefinition.id] = serverDefinition
            when (serverDefinition) {
                is ArtifactDefinition -> {
                    rootPanel.add(
                        createArtifactRow(
                            serverDefinition.id,
                            serverDefinition.extensions,
                            serverDefinition.packge,
                            serverDefinition.mainClass,
                            serverDefinition.args.joinToString(" ")
                        )
                    )
                }
                is ExeDefinition -> {
                    rootPanel.add(
                        createExeRow(
                            serverDefinition.id,
                            serverDefinition.extensions,
                            serverDefinition.path,
                            serverDefinition.args.joinToString(" ")
                        )
                    )
                }
                is RawCommandDefinition -> {
                    rootPanel.add(createCommandRow(serverDefinition.id, serverDefinition.extensions, serverDefinition.command.joinToString(" ")))
                }
                else -> {
                    logger.error("Unknown UserConfigurableServerDefinition : $serverDefinition")
                }
            }
        }
    }

    override fun apply() {
        val ids = rows.map { row -> row.getText(CommonDefinitionKey.ID) }
        val nonUniqueIds = removeUnique(ids)
        if (nonUniqueIds.isNotEmpty()) {
            Messages.showWarningDialog(project, nonUniqueIds.reduce { f, s -> "Duplicate : $f${Utils.LINE_SEPARATOR}$s" }, "Duplicate IDs")
        } else {
            val exts = rows.flatMap { row -> row.getText(CommonDefinitionKey.EXT).split(Definition.SPLIT_CHAR) }
            val nonUniqueExts = removeUnique(exts)
            if (nonUniqueExts.isNotEmpty()) {
                Messages.showWarningDialog(project, nonUniqueExts.reduce { f, s -> "Duplicate : $f${Utils.LINE_SEPARATOR}$s" }, "Duplicate Extensions")
            } else {
                serverDefinitions.clear()
                for (row in rows) {
                    val arr = row.toCSVLines().toList()
                    val id = row.getText(CommonDefinitionKey.ID)
                    val serverDefinition = definitionProvider.getLanguageServerDefinition(arr)
                    if (serverDefinition != null) {
                        serverDefinitions[id] = serverDefinition
                    }
                }
                projectSettings.projectState = projectSettings.projectState.withObjectDefinitions(serverDefinitions)
            }
        }
    }

    override fun isModified(): Boolean {
        return if (serverDefinitions.size == rows.filter { row -> row.toCSVLines().drop(1).any { (csvLine) -> csvLine.isNotEmpty() } }.size) {
            for (row in rows) {
                val stateDef = serverDefinitions[row.getText(CommonDefinitionKey.ID)]
                val rowDef = definitionProvider.getLanguageServerDefinition(row.toCSVLines())
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
        val state = project.service<LSPPersistentProjectSettings>().projectState
        if (state.idToDefinition.isNotEmpty()) {
            for (serverDefinition in state.idToDefinition.values) {
                addServerDefinition(serverDefinition)
            }
        } else {
            rootPanel.add(createArtifactRow("", emptyList(), "", "", ""))
        }
    }

    private fun createComboBox(panel: JPanel, selected: String): JComboBox<String> {
        val typeBox = ComboBox<String>()
        val types = ConfigurableTypes.values()
        for (type in types) {
            typeBox.addItem(type.type)
        }
        typeBox.selectedItem = selected
        typeBox.addItemListener { e: ItemEvent ->
            if (e.stateChange == ItemEvent.SELECTED) {
                val idx = getComponentIndex(panel)
                when (e.item) {
                    ConfigurableTypes.ARTIFACT.type -> {
                        rootPanel.add(createArtifactRow("", emptyList(), "", "", ""), idx)
                        rootPanel.remove(panel)
                        rows.removeAt(idx)
                    }
                    ConfigurableTypes.RAWCOMMAND.type -> {
                        rootPanel.add(createCommandRow("", emptyList(), ""), idx)
                        rootPanel.remove(panel)
                        rows.removeAt(idx)
                    }
                    ConfigurableTypes.EXE.type -> {
                        rootPanel.add(createExeRow("", emptyList(), "", ""), idx)
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
        newRowButton.addActionListener { rootPanel.add(createArtifactRow("", emptyList(), "", "", "")) }
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


    private fun createServerSettingsButton(): JButton {
        val button = JButton()
        button.text = "Settings..."
        button.addActionListener {
            AdvancedServerSettingsGUI(project, "")
        }
        return button
    }

    private fun createRow(labelFields: Collection<JComponent>, selectedItem: String): JPanel {
        val panel: JBPanel<*> = JBPanel<JBPanel<*>>(GridLayoutManager(2, 21, JBUI.emptyInsets(), -1, -1))
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
                1,
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
        var colIdx = 2
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
                16,
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
        val serverSettingsButton = createServerSettingsButton()
        panel.add(
            serverSettingsButton,
            GridConstraints(
                0,
                17,
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
        panel.add(
            Spacer(),
            GridConstraints(
                0,
                18,
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
                19,
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
                    20,
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
                    20,
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


    private fun createArtifactRow(id: String, extensions: Iterable<String>, serv: String, mainClass: String, args: String): JPanel {
        val packgeLabel: JLabel = JBLabel("Artifact")
        val packgeField = JTextArea()
        packgeField.lineWrap = true
        packgeField.toolTipText = "e.g. ch.epfl.lamp:dotty-language-server_0.3:0.3.0-RC2"
        packgeField.text = serv
        val mainClassLabel = JBLabel("Main class")
        val mainClassField = JTextArea()
        mainClassField.lineWrap = true
        mainClassField.toolTipText = "e.g. dotty.tools.languageserver.Main"
        mainClassField.text = mainClass
        val argsLabel: JLabel = JBLabel("Args")
        val argsField = JTextArea()
        argsField.lineWrap = true
        argsField.toolTipText = "e.g. -stdio"
        argsField.text = args
        val baseRow = createBaseRow(id, extensions)
        val components = baseRow.first + listOf<JComponent>(packgeLabel, packgeField, mainClassLabel, mainClassField, argsLabel, argsField)
        val panel = createRow(components, ArtifactDefinition.presentableType)
        val map = baseRow.second
        map[ArtifactDefinition.Key.PACKAGE] = packgeField
        map[ArtifactDefinition.Key.MAIN_CLASS] = mainClassField
        map[ArtifactDefinition.Key.ARGS] = argsField
        rows.add(ServersGUIRow(id, ArtifactDefinition.type, project, map))
        return panel
    }

    private fun createExeRow(id: String, extensions: Iterable<String>, path: String, args: String): JPanel {
        val pathLabel = JBLabel(FILE_PATH_LABEL)
        val pathField = TextFieldWithBrowseButton()
        pathField.toolTipText = "e.g. C:\\rustLS\\rls.exe"
        pathField.text = path
        pathField.addBrowseFolderListener(TextBrowseFolderListener(FileChooserDescriptor(true, false, true, true, true, false).withShowHiddenFiles(true)))
        val argsLabel = JBLabel("Args")
        val argsField = JTextArea()
        argsField.lineWrap = true
        argsField.toolTipText = "e.g. -stdio"
        argsField.text = args
        val baseRow = createBaseRow(id, extensions)
        val components = baseRow.first + listOf<JComponent>(pathLabel, pathField, argsLabel, argsField)
        val panel = createRow(components, ExeDefinition.presentableType)
        val map = baseRow.second
        map[ExeDefinition.Key.PATH] = pathField
        map[ExeDefinition.Key.ARGS] = argsField
        rows.add(ServersGUIRow(id, ExeDefinition.type, project, map))
        return panel
    }

    private fun createCommandRow(id: String, extensions: Iterable<String>, command: String): JPanel {
        val commandLabel = JBLabel("Command")
        val argsField = JTextArea()
        argsField.lineWrap = true
        argsField.text = command
        argsField.toolTipText = "e.g. python.exe -m C:\\python-ls\\pyls"
        val baseRow = createBaseRow(id, extensions)
        val components = baseRow.first + listOf<JComponent>(commandLabel, argsField)
        val panel = createRow(components, RawCommandDefinition.presentableType)
        val map = baseRow.second
        map[RawCommandDefinition.Key.COMMAND] = argsField
        rows.add(ServersGUIRow(id, RawCommandDefinition.type, project, map))
        return panel
    }

    private fun createBaseRow(id: String, extensions: Iterable<String>): Pair<List<JComponent>, MutableMap<DefinitionKey, JComponent>> {
        val idLabel = JBLabel(ID_LABEL)
        val idField = JBTextField()
        idField.toolTipText = ID_TOOLTIP
        idField.text = id
        val extLabel = JBLabel(EXTENSION_LABEL)
        val extField = JBTextField()
        extField.toolTipText = EXTENSION_TOOLTIP
        extField.text = extensions.joinToString { Definition.SPLIT_CHAR }
        return Pair(listOf(idLabel, idField, extLabel, extField), mutableMapOf(CommonDefinitionKey.ID to idField, CommonDefinitionKey.EXT to extField))
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
        private const val ID_LABEL = "ID"
        private const val ID_TOOLTIP = "id of the server, must be unique"
        private const val EXTENSION_LABEL = "Extension"
        private const val EXTENSION_TOOLTIP = "e.g. scala, java, c, js, ..."
        private const val FILE_PATH_LABEL = "Path"
        private val logger = Logger.getInstance(ServersSettingsGUI::class.java)

        private fun <T> removeUnique(iter: Iterable<T>): List<T> {
            val orig = iter.toMutableList()
            orig.distinct().forEach { v -> orig.remove(v) }
            return orig
        }
    }
}