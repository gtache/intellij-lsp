package com.github.gtache.lsp.actions

import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.settings.LSPState
import com.intellij.codeInsight.documentation.actions.ShowQuickDocInfoAction
import com.intellij.lang.LanguageDocumentation
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiManager

/**
 * Action overriding QuickDoc (CTRL+Q)
 */
class LSPQuickDocAction : ShowQuickDocInfoAction(), DumbAware {
    companion object {
        private val logger: Logger = Logger.getInstance(LSPQuickDocAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent): Unit {
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            val file = FileDocumentManager.getInstance().getFile(editor.document)
            val project = editor.project
            if (file != null && project != null) {
                val language = PsiManager.getInstance(project).findFile(file)?.language
                //Hack for IntelliJ 2018 TODO proper way
                if (language != null && (LSPState.instance?.isAlwaysSendRequests != false ||
                            LanguageDocumentation.INSTANCE.allForLanguage(language).isEmpty() ||
                            (ApplicationInfo.getInstance().majorVersion.toInt() > 2017) && PlainTextLanguage.INSTANCE == language)) {
                    val manager = EditorEventManager.forEditor(editor)
                    if (manager != null) {
                        manager.quickDoc(editor)
                    } else super.actionPerformed(e)
                } else super.actionPerformed(e)
            } else super.actionPerformed(e)
        } else super.actionPerformed(e)
    }
}