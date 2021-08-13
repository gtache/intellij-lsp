package com.github.gtache.lsp.actions

import com.github.gtache.lsp.PluginMain
import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.settings.LSPState
import com.intellij.codeInsight.actions.LayoutCodeDialog
import com.intellij.codeInsight.actions.ShowReformatFileDialog
import com.intellij.codeInsight.actions.TextRangeType
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiDocumentManager

/**
 * Class overriding the default action handling the Reformat dialog event (CTRL+ALT+SHIFT+L by default)
 * Fallback to the default action if the language is already supported or not supported by any language server
 */
class LSPShowReformatDialogAction : ShowReformatFileDialog(), DumbAware {
    companion object {
        private const val HELP_ID = "editing.codeReformatting"
        private val logger: Logger = Logger.getInstance(LSPShowReformatDialogAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent): Unit {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val project = e.getData(CommonDataKeys.PROJECT)
        if (editor != null && project != null) {
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            if (file != null && (LSPState.instance?.isAlwaysSendRequests != false ||
                        (LanguageFormatting.INSTANCE.allForLanguage(file.language).isEmpty()
                                && PluginMain.isExtensionSupported(
                            FileDocumentManager.getInstance().getFile(editor.document)?.extension
                        )))
            ) {

                val hasSelection = editor.selectionModel.hasSelection()
                val dialog = LayoutCodeDialog(project, file, hasSelection, HELP_ID)
                dialog.show()

                if (dialog.isOK) {
                    val options = dialog.runOptions
                    EditorEventManager.forEditor(editor)
                        ?.let { manager -> if (options.textRangeType == TextRangeType.SELECTED_TEXT) manager.reformatSelection() else manager.reformat() }
                }
            } else {
                super.actionPerformed(e)
            }
        } else {
            super.actionPerformed(e)
        }
    }

}