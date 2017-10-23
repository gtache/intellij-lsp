package com.github.gtache.contributors

import com.github.gtache.contributors.psi.LSPPsiElement
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement

class LSPRefactoringSupportProvider extends RefactoringSupportProvider {

  override def isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement): Boolean = element.isInstanceOf[LSPPsiElement]
}
