package com.github.gtache.lsp.contributors

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement

/**
 * A findUsagesProvider for LSP (ALT+F7)
 */
class LSPFindUsagesProvider : FindUsagesProvider {

    companion object {
        private val logger: Logger = Logger.getInstance(LSPFindUsagesProvider::class.java)
    }

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return when (psiElement) {
            is PsiFile -> true
            is LSPPsiElement -> true
            else -> false
        }
    }

    override fun getWordsScanner(): WordsScanner? = null

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = element.text

    override fun getDescriptiveName(element: PsiElement): String = if (element is PsiNamedElement) element.name ?: "" else ""

    override fun getType(element: PsiElement): String = ""
}