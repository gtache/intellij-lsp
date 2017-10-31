package com.github.gtache.contributors

import java.util

import com.github.gtache.editor.EditorEventManager
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.{PsiElement, PsiManager}

/**
  * A documentation provider for LSP (is called when CTRL is pushed while staying on a token)
  */
class LSPDocumentationProvider extends DocumentationProvider {
  private val LOG: Logger = Logger.getInstance(classOf[LSPDocumentationProvider])


  override def getUrlFor(element: PsiElement, originalElement: PsiElement): java.util.List[String] = {
    new util.ArrayList[String]()
  }

  override def getDocumentationElementForLookupItem(psiManager: PsiManager, obj : scala.Any, element: PsiElement): PsiElement = {
    null
  }

  override def getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement): PsiElement = {
    null
  }

  override def generateDoc(element: PsiElement, originalElement: PsiElement): String = {
    getQuickNavigateInfo(element, originalElement)
  }

  override def getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement): String = {
    var editor: Editor = null
    ApplicationManager.getApplication.invokeAndWait(() => {
      editor = FileEditorManager.getInstance(originalElement.getProject).getSelectedTextEditor
    })
    EditorEventManager.forEditor(editor).fold("")(e => e.requestDoc(editor, originalElement.getTextOffset))
  }
}
