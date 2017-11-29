package com.github.gtache.lsp.contributors


import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util
import com.intellij.openapi.util.TextRange
import com.intellij.psi.{PsiElement, PsiNamedElement}
import com.intellij.refactoring.rename.inplace.InplaceRefactoring

class LSPInplaceRenamer(editor: Editor, element: PsiNamedElement, project: Project, oldName : String) extends InplaceRefactoring(editor,element,project,oldName) {
  override def shouldSelectAll() = ???

  override def performRefactoring() = ???

  override def collectAdditionalElementsToRename(stringUsages: java.util.List[util.Pair[PsiElement, TextRange]]) = ???

  override def getCommandName = ???
}
