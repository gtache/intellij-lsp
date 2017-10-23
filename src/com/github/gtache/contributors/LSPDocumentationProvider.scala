package com.github.gtache.contributors

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.{PsiElement, PsiManager}

class LSPDocumentationProvider extends DocumentationProvider {
  override def getUrlFor(element: PsiElement, originalElement: PsiElement): java.util.List[String] = ???

  override def getDocumentationElementForLookupItem(psiManager: PsiManager, `object`: scala.Any, element: PsiElement): PsiElement = ???

  override def getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement): PsiElement = ???

  override def getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement): String = ???

  override def generateDoc(element: PsiElement, originalElement: PsiElement): String = ???
}
