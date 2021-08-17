package com.github.gtache.lsp.contributors.inspection

import com.github.gtache.lsp.contributors.fixes.CodeActionFix
import com.github.gtache.lsp.contributors.fixes.CommandFix
import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.editor.services.project.EditorProjectService
import com.github.gtache.lsp.services.project.LSPProjectService
import com.github.gtache.lsp.utils.DocumentUtils.getTextClamped
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.components.service
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.DiagnosticSeverity
import javax.swing.JComponent

/**
 * The inspection tool for LSP
 */
class LSPInspection : LocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val virtualFile = file.virtualFile
        val ext = virtualFile.extension
        return if (ext != null && file.project.service<LSPProjectService>().isExtensionSupported(ext)) {
            val uri = FileUtils.VFSToURI(virtualFile)

            /**
             * Get all the ProblemDescriptor given an EditorEventManager
             * Look at the DiagnosticHighlights, create dummy PsiElement for each, create descriptor using it
             *
             * @param m The manager
             * @return The ProblemDescriptors
             */
            fun descriptorsForManager(m: EditorEventManager): Array<ProblemDescriptor> {
                val diagnostics = m.getDiagnostics()
                return diagnostics.mapNotNull { drh ->
                    val rangeHighlighter = drh.rangeHighlighter
                    val diagnostic = drh.diagnostic
                    val start = rangeHighlighter.startOffset
                    val end = rangeHighlighter.endOffset
                    if (start < end) {
                        val name = m.editor.document.getTextClamped(start, end)
                        val severity = when (diagnostic.severity) {
                            DiagnosticSeverity.Error -> ProblemHighlightType.ERROR
                            DiagnosticSeverity.Warning -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                            DiagnosticSeverity.Information -> ProblemHighlightType.INFORMATION
                            DiagnosticSeverity.Hint -> ProblemHighlightType.INFORMATION
                            else -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        }
                        m.editor.project?.let {
                            val element = LSPPsiElement(name, it, start, end, file, m.editor)
                            val codeActionResult = m.codeAction(element)
                            val fixes = if (codeActionResult != null) {
                                val (commandsE, codeActionsE) = codeActionResult.filter { e -> e.isLeft || e.isRight }
                                    .partition { e -> e.isLeft }
                                uri?.let {
                                    val commands = commandsE.map { e -> e.left }.map { c -> CommandFix(uri, c) }
                                    val codeActions = codeActionsE.map { e -> e.right }.map { c -> CodeActionFix(uri, c) }
                                    (commands + codeActions).toTypedArray()
                                } ?: emptyArray()
                            } else emptyArray()
                            manager.createProblemDescriptor(element, null, diagnostic.message, severity, isOnTheFly, *fixes)
                        }
                    } else null
                }.toTypedArray()
            }

            uri?.let {
                val m = file.project.service<EditorProjectService>().forUri(it)
                if (m != null) {
                    descriptorsForManager(m)
                } else {
                    if (isOnTheFly) {
                        super.checkFile(file, manager, isOnTheFly)
                    } else {
                        /*val descriptor = OpenFileDescriptor(manager.getProject, virtualFile)
                        ApplicationUtils.writeAction(() -> FileEditorManager.getInstance(manager.getProject).openTextEditor(descriptor, false))
                        EditorEventManager.forUri(uri) when {
                          Some(m) -> descriptorsForManager(m)
                          None -> super.checkFile(file, manager, isOnTheFly)
                        }*/
                        //TODO need dispatch thread
                        super.checkFile(file, manager, isOnTheFly)
                    }
                }
            } ?: super.checkFile(file, manager, isOnTheFly)
        } else super.checkFile(file, manager, isOnTheFly)
    }

    override fun getDisplayName(): String = shortName

    override fun createOptionsPanel(): JComponent {
        return LSPInspectionPanel(shortName, this)
    }

    override fun getShortName(): String = "LSP"

    override fun getID(): String = "LSP"

    override fun getGroupDisplayName(): String = "LSP"

    override fun getStaticDescription(): String = "Reports errors by the LSP server"

    override fun isEnabledByDefault(): Boolean = true

}