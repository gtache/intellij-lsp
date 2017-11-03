package com.github.gtache.contributors

import com.github.gtache.editor.EditorEventManager
import com.github.gtache.utils.Utils
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.{PsiElement, PsiManager}

/**
  * A documentation provider for LSP (is called when CTRL is pushed while staying on a token)
  */
class LSPDocumentationProvider extends DocumentationProvider {
  private val LOG: Logger = Logger.getInstance(classOf[LSPDocumentationProvider])


  override def getUrlFor(element: PsiElement, originalElement: PsiElement): java.util.List[String] = {
    null
  }

  override def getDocumentationElementForLookupItem(psiManager: PsiManager, obj: scala.Any, element: PsiElement): PsiElement = {
    null
  }

  override def getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement): PsiElement = {
    null
  }

  override def generateDoc(element: PsiElement, originalElement: PsiElement): String = {
    getQuickNavigateInfo(element, originalElement)
  }

  override def getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement): String = {
    val uri = Utils.VFSToURIString(originalElement.getContainingFile.getVirtualFile)
    EditorEventManager.forUri(uri).fold("")(e => e.requestDoc(e.editor, originalElement.getTextOffset))
  }
}