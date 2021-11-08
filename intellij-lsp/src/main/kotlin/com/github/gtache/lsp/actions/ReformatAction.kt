package com.github.gtache.lsp.actions

import com.github.gtache.lsp.requests.services.project.ReformatProjectService
import com.github.gtache.lsp.services.project.LSPProjectService
import com.github.gtache.lsp.settings.project.LSPProjectSettings
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
class ReformatAction : ReformatCodeAction(), DumbAware {

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
                project.service<ReformatProjectService>().reformatFile(editor)
            } else {
                super.actionPerformed(e)
            }
        }
    }

}