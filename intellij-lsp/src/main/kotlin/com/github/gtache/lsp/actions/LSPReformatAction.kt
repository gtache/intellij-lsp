package com.github.gtache.lsp.actions

import com.github.gtache.lsp.LSPProjectService
import com.github.gtache.lsp.requests.ReformatHandler
import com.github.gtache.lsp.settings.LSPProjectSettings
import com.intellij.codeInsight.actions.ReformatCodeAction
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiDocumentManager

/**
 * Action overriding the default reformat action
 * Fallback to the default action if the language is already supported or not supported by any language server
 */
class LSPReformatAction : ReformatCodeAction(), DumbAware {

    companion object {
        private val logger: Logger = Logger.getInstance(LSPReformatAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent): Unit {
        val project = e.getData(CommonDataKeys.PROJECT)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null && project != null) {
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            val state = project.service<LSPProjectSettings>().projectState
            if (file != null && (state.isAlwaysSendRequests ||
                        (LanguageFormatting.INSTANCE.allForLanguage(file.language).isEmpty()
                                && project.service<LSPProjectService>().isExtensionSupported(file.virtualFile.extension)))
            ) {
                ReformatHandler.reformatFile(editor)
            } else {
                super.actionPerformed(e)
            }
        }
    }

}