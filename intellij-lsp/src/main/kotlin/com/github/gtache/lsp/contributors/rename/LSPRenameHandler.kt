package com.github.gtache.lsp.contributors.rename

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.services.application.EditorApplicationService
import com.github.gtache.lsp.services.project.LSPProjectService
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.impl.StartMarkAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.rename.RenameHandler
import com.intellij.refactoring.rename.RenameHandlerRegistry
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.inplace.InplaceRefactoring
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.MemberInplaceRenamer

/**
 * Rename handler for LSP
 */
class LSPRenameHandler : RenameHandler {

    override fun invoke(project: Project, elements: Array<PsiElement>, dataContext: DataContext): Unit {
        dataContext.getData(CommonDataKeys.EDITOR)?.let { editor ->
            if (elements.size == 1) {
                MemberInplaceRenameHandler().doRename(elements[0], editor, dataContext)
            } else dataContext.getData(CommonDataKeys.PSI_FILE)?.let { invoke(project, editor, it, dataContext) }
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext): Unit {
        val manager = service<EditorApplicationService>().managerForEditor(editor)
        if (manager != null) {
            if (editor.contentComponent.hasFocus()) {
                val psiElement = manager.getElementAtOffset(editor.caretModel.currentCaret.offset)
                if (psiElement != null) {
                    doRename(psiElement, editor, dataContext)
                }
            }
        }
    }

    private fun doRename(elementToRename: PsiElement, editor: Editor, dataContext: DataContext): InplaceRefactoring? {
        if (elementToRename is PsiNameIdentifierOwner) {
            val processor = RenamePsiElementProcessor.forElement(elementToRename)
            if (processor.isInplaceRenameSupported) {
                val startMarkAction = StartMarkAction.canStart(editor)
                if (startMarkAction == null || (processor.substituteElementToRename(elementToRename, editor) === elementToRename)) {
                    processor.substituteElementToRename(elementToRename, editor, Pass.create { e ->
                        val renamer = createMemberRenamer(e, elementToRename, editor)
                        val startedRename = renamer.performInplaceRename()
                        if (!startedRename) performDialogRename(elementToRename, editor, dataContext)
                    })
                    return null
                } else {
                    val inplaceRefactoring = editor.getUserData(InplaceRefactoring.INPLACE_RENAMER)
                    if (inplaceRefactoring != null && (inplaceRefactoring.javaClass === MemberInplaceRenamer::class.java)) {
                        TemplateManagerImpl.getTemplateState(InjectedLanguageEditorUtil.getTopLevelEditor(editor))?.gotoEnd(true)
                    }
                }
            }
        }
        performDialogRename(elementToRename, editor, dataContext)
        return null
    }

    private fun performDialogRename(elementToRename: PsiElement, editor: Editor, dataContext: DataContext): Unit {
        LSPRenameHelper.rename(editor)
    }

    private fun createMemberRenamer(element: PsiElement, elementToRename: PsiNameIdentifierOwner, editor: Editor): MemberInplaceRenamer {
        return LSPInplaceRenamer(element as PsiNamedElement, elementToRename, editor)
    }

    override fun isRenaming(dataContext: DataContext): Boolean {
        return isAvailableOnDataContext(dataContext)
    }

    private fun checkAvailable(elementToRename: PsiElement, editor: Editor, dataContext: DataContext): Boolean {
        return if (!isAvailableOnDataContext(dataContext)) {
            RenameHandlerRegistry.getInstance().getRenameHandler(dataContext)
                ?.invoke(elementToRename.project, editor, elementToRename.containingFile, dataContext)
            false
        } else {
            true
        }
    }

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val element = PsiElementRenameHandler.getElement(dataContext)
        val editor = CommonDataKeys.EDITOR.getData(dataContext)
        val file = CommonDataKeys.PSI_FILE.getData(dataContext)
        if (editor == null || file == null) return false
        return isAvailable(element, editor, file)
    }

    private fun isAvailable(psiElement: PsiElement?, editor: Editor, psiFile: PsiFile): Boolean {
        return when (psiElement) {
            is PsiFile -> {
                service<EditorApplicationService>().managerForEditor(editor)?.canRename() ?: false
            }
            is LSPPsiElement -> {
                service<EditorApplicationService>().managerForEditor(editor)?.canRename(psiElement.textOffset) ?: false
                //IntelliJ 2018 returns psiElement null for unsupported languages
            }
            else -> {
                val project = editor.project ?: psiFile.project

                psiElement == null && FileUtils.psiFileToExtension(psiFile)?.let { project.service<LSPProjectService>().isExtensionSupported(it) } ?: false
            }
        }
    }

}