package com.github.gtache.lsp.contributors

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.codeInspection.{LocalQuickFix, ProblemDescriptor}
import com.intellij.openapi.project.Project

class LSPQuickFix(uri: String) extends LocalQuickFix {
  override def applyFix(project: Project, descriptor: ProblemDescriptor): Unit = {
    descriptor.getPsiElement match {
      case element: LSPPsiElement =>
        EditorEventManager.forUri(uri) match {
          case Some(m) =>
            m.codeAction(element)
          case None =>
        }
      case _ =>
    }
  }

  override def getFamilyName: String = "LSP Fix"
}
