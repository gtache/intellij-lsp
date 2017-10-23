package com.github.gtache.contributors

import com.github.gtache.PluginMain
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi._
import com.intellij.util.ProcessingContext

/**
  * The reference contributor for LSP
  */
class LSPReferenceContributor extends PsiReferenceContributor {
  override def registerReferenceProviders(registrar: PsiReferenceRegistrar): Unit = {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(classOf[PsiLiteralExpression]), (element: PsiElement, context: ProcessingContext) => {
      val editor = context.get(CommonDataKeys.EDITOR).asInstanceOf[Editor]
      val lpos = editor.offsetToLogicalPosition(element.getTextOffset)
      PluginMain.references(editor, lpos)
    })
  }

  private def LOG: Logger = Logger.getInstance(classOf[LSPReferenceContributor])
}
