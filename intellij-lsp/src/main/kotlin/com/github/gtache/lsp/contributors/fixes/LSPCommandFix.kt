package com.github.gtache.lsp.contributors.fixes

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.EditorApplicationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.Command

/**
 * The Quickfix for LSP
 *
 * @param uri     The file in which the commands will be applied
 * @param command The command to run
 */
class LSPCommandFix(private val uri: String, private val command: Command) : LocalQuickFix {

    override fun applyFix(project: Project, descriptor: ProblemDescriptor): Unit {
        val psiElement = descriptor.psiElement
        if (psiElement is LSPPsiElement) {
            service<EditorApplicationService>().forEditor(psiElement.editor)?.executeCommands(listOf(command))
        }
    }

    override fun getFamilyName(): String = "LSP Fixes"

    override fun getName(): String {
        return command.title
    }

    companion object {
        private val logger: Logger = Logger.getInstance(LSPCommandFix::class.java)
    }
}