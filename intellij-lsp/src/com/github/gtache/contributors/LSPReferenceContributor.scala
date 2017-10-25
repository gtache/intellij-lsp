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
  private val LOG: Logger = Logger.getInstance(classOf[LSPReferenceContributor])

  override def registerReferenceProviders(registrar: PsiReferenceRegistrar): Unit = {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(classOf[PsiLiteralExpression]), (element: PsiElement, context: ProcessingContext) => {
      val editor = context.get(CommonDataKeys.EDITOR).asInstanceOf[Editor]
      if (editor != null) {
        LOG.info("Reference for editor " + editor)
        val lpos = editor.offsetToLogicalPosition(element.getTextOffset)
        PluginMain.references(editor, lpos)
      } else {
        LOG.info("No editor for LSPReferenceContributor")
        Array()
      }
    })
  }
}
