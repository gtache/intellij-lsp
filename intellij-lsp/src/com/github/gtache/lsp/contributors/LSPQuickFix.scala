package com.github.gtache.lsp.contributors

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.intellij.codeInspection.{LocalQuickFix, ProblemDescriptor}
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.Command

/**
  * The Quickfix for LSP
  *
  * @param uri      The file in which the commands will be applied
  * @param commands The commands to run
  */
class LSPQuickFix(uri: String, commands: Iterable[Command]) extends LocalQuickFix {
  override def applyFix(project: Project, descriptor: ProblemDescriptor): Unit = {
    descriptor.getPsiElement match {
      case element: LSPPsiElement =>
      //applyCommand
      case _ =>
    }
  }

  override def getFamilyName: String = "LSP Fix"
}
