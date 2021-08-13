package com.github.gtache.lsp.contributors.inspection

import com.intellij.codeInspection.InspectionProfileEntry
import java.awt.GridBagLayout
import javax.swing.JPanel

/**
 * The Options panel for the LSP inspection
 *
 * @param label
 * @param owner
 */
class LSPInspectionPanel(label: String, owner: InspectionProfileEntry) : JPanel(GridBagLayout())