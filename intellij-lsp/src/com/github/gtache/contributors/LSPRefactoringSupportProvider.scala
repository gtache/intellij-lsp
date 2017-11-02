package com.github.gtache.contributors

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.{PsiElement, PsiNamedElement}

/**
  * The refactoringSupportProvider for LSP
  */
class LSPRefactoringSupportProvider extends RefactoringSupportProvider {

  override def isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement): Boolean = element.isInstanceOf[PsiNamedElement]
}
