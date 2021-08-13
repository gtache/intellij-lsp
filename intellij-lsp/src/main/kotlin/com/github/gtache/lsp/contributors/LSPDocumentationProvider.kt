package com.github.gtache.lsp.contributors

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

/**
 * A documentation provider for LSP (is called when CTRL is pushed while staying on a token)
 */
class LSPDocumentationProvider : DocumentationProvider {
    companion object {
        private val logger: Logger = Logger.getInstance(LSPDocumentationProvider::class.java)
    }

    override fun getUrlFor(element: PsiElement, originalElement: PsiElement): MutableList<String>? {
        return null
    }

    override fun getDocumentationElementForLookupItem(psiManager: PsiManager, obj: Any, element: PsiElement): PsiElement? {
        return null
    }

    override fun getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement): PsiElement? {
        return null
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String {
        return getQuickNavigateInfo(element, originalElement)
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String {
        return when (element) {
            is LSPPsiElement ->
                FileUtils.VFSToURI(element.containingFile.virtualFile)?.let {
                    EditorEventManager.forUri(it)?.let { m ->
                        m.requestDoc(m.editor, element.getTextOffset())
                    }
                } ?: ""
            is PsiFile -> {
                val editor = FileUtils.editorFromPsiFile(element)
                editor?.let {
                    EditorEventManager.forEditor(it)?.requestDoc(it, it.caretModel.currentCaret.offset)
                } ?: ""
            }
            else -> ""
        }
    }
}