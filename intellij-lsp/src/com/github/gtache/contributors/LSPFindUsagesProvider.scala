package com.github.gtache.contributors

import com.github.gtache.contributors.psi.LSPPsiElement
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.{PsiElement, PsiNamedElement}

/**
  * A findUsagesProvider for LSP (ALT+F7)
  */
class LSPFindUsagesProvider extends FindUsagesProvider {

  override def getHelpId(psiElement: PsiElement): String = null

  override def canFindUsagesFor(psiElement: PsiElement): Boolean = true

  override def getWordsScanner: WordsScanner = null

  override def getNodeText(element: PsiElement, useFullName: Boolean): String = element.getText

  override def getDescriptiveName(element: PsiElement): String = element match {
    case e: PsiNamedElement => e.getName
    case _ => ""
  }

  override def getType(element: PsiElement): String = element match {
    case LSPPsiElement(_, _, _, _) => "LSP element"
    case _ => ""
  }

  private def LOG: Logger = Logger.getInstance(classOf[LSPFindUsagesProvider])
}
