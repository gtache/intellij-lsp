package com.github.gtache.lsp.contributors.fixes

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.EditorApplicationService
import com.github.gtache.lsp.requests.WorkspaceEditHandler
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.CodeAction

/**
 * The Quickfix for LSP
 *
 * @param uri        The file in which the commands will be applied
 * @param codeAction The action to execute
 */
class CodeActionFix(private val uri: String, private val codeAction: CodeAction) : LocalQuickFix {

    override fun applyFix(project: Project, descriptor: ProblemDescriptor): Unit {
        val psiElement = descriptor.psiElement
        if (psiElement is LSPPsiElement) {
            if (codeAction.edit != null) WorkspaceEditHandler.applyEdit(codeAction.edit, project, codeAction.title)
            service<EditorApplicationService>().forEditor(psiElement.editor)?.executeCommands(listOf(codeAction.command))
        }
    }

    override fun getFamilyName(): String = "LSP Fixes"

    override fun getName(): String {
        return codeAction.title
    }

    companion object LSPCodeActionFix {
        private val logger: Logger = Logger.getInstance(CodeActionFix::class.java)
    }
}

