package com.github.gtache.lsp.contributors

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.{PsiElement, PsiFile}
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.rename.RenameHandler

class LSPRenameHandler extends RenameHandler {
  override def invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) = ???

  override def invoke(project: Project, elements: Array[PsiElement], dataContext: DataContext) = ???

  override def isAvailableOnDataContext(dataContext: DataContext) = true

  override def isRenaming(dataContext: DataContext) = false
}
