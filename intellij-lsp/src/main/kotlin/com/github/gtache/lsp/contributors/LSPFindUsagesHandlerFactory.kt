package com.github.gtache.lsp.contributors

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.services.application.EditorApplicationService
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope

class LSPFindUsagesHandlerFactory : FindUsagesHandlerFactory() {


    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler {
        return object : FindUsagesHandler(element) {
            @Volatile
            private var elements: Iterable<PsiElement> = ArrayList()

            init {
                fun setElemsFromEditor(editor: Editor?) {
                    if (editor != null) {
                        val manager = service<EditorApplicationService>().forEditor(editor)
                        if (manager != null) {
                            ApplicationUtils.invokeLater {
                                if (!editor.isDisposed) {
                                    elements = manager.references(
                                        editor.caretModel.currentCaret.offset,
                                        getOriginalElement = true
                                    ).first
                                }
                            }
                        }
                    }
                }
                when (element) {
                    is PsiFile -> {
                        setElemsFromEditor(FileUtils.editorFromPsiFile(element))
                    }
                    is LSPPsiElement -> {
                        setElemsFromEditor(FileUtils.editorFromPsiFile(element.containingFile))
                    }
                }
            }

            override fun findReferencesToHighlight(target: PsiElement, searchScope: SearchScope): Collection<PsiReference> {
                return if (target is PsiFile && target == element) {
                    elements.mapNotNull { p -> p.reference }
                } else if (target is LSPPsiElement && elements.any { p -> p == target }) {
                    elements.mapNotNull { p -> p.reference }
                } else {
                    emptyList()
                }
            }

            override fun getPrimaryElements(): Array<PsiElement> {
                return elements.toList().toTypedArray()
            }
        }
    }

    override fun canFindUsages(element: PsiElement): Boolean {
        return when (element) {
            is PsiFile -> true
            is LSPPsiElement -> true
            else -> false
        }
    }
}