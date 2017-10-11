package com.github.gtache.contributors

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi._
import com.intellij.util.ProcessingContext

/**
  * The reference contributor for LSP
  */
class LSPReferenceContributor extends PsiReferenceContributor {
  override def registerReferenceProviders(registrar: PsiReferenceRegistrar): Unit = {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(classOf[PsiLiteralExpression]), (element: PsiElement, context: ProcessingContext) => {
      ???
    })
  }
}
