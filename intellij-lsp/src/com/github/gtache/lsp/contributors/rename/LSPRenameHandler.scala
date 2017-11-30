package com.github.gtache.lsp.contributors.rename

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.actionSystem.{CommonDataKeys, DataContext}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.{PsiElement, PsiFile, PsiNameIdentifierOwner, PsiNamedElement}
import com.intellij.refactoring.rename.inplace.{MemberInplaceRenameHandler, MemberInplaceRenamer}

class LSPRenameHandler extends MemberInplaceRenameHandler {
  override def invoke(project: Project, elements: Array[PsiElement], dataContext: DataContext): Unit = {
    if (elements.length == 1) new MemberInplaceRenameHandler().doRename(elements(0), dataContext.getData(CommonDataKeys.EDITOR), dataContext)
    else invoke(project, dataContext.getData(CommonDataKeys.EDITOR), dataContext.getData(CommonDataKeys.PSI_FILE), dataContext)
  }

  override def invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext): Unit = {
    EditorEventManager.forEditor(editor) match {
      case Some(m) =>
        if (editor.getContentComponent.hasFocus) {
          val psiElement = m.getElementAtOffset(editor.getCaretModel.getCurrentCaret.getOffset)
          if (psiElement != null) {
            doRename(psiElement, editor, dataContext)
          }
        }
      case None =>
    }
  }


  override def isAvailable(psiElement: PsiElement, editor: Editor, psiFile: PsiFile): Boolean = {
    psiElement match {
      case p: PsiFile => true
      case l: LSPPsiElement => true
      case _ => false
    }
  }

  override def createMemberRenamer(element: PsiElement, elementToRename: PsiNameIdentifierOwner, editor: Editor): MemberInplaceRenamer = {
    new LSPInplaceRenamer(element.asInstanceOf[PsiNamedElement], elementToRename, editor)()
  }

}
