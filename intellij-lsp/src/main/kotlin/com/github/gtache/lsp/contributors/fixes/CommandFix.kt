package com.github.gtache.lsp.contributors.fixes

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.services.application.EditorApplicationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.Command

/**
 * A Quickfix using a command
 *
 * @param uri     The file in which the commands will be applied
 * @param command The command to run
 */
class CommandFix(private val uri: String, private val command: Command) : LocalQuickFix {

    override fun applyFix(project: Project, descriptor: ProblemDescriptor): Unit {
        val psiElement = descriptor.psiElement
        if (psiElement is LSPPsiElement) {
            service<EditorApplicationService>().managerForEditor(psiElement.editor)?.executeCommands(listOf(command))
        }
    }

    override fun getFamilyName(): String = "LSP Fixes"

    override fun getName(): String {
        return command.title
    }
}