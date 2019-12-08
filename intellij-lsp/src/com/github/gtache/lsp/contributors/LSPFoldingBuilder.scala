package com.github.gtache.lsp.contributors

import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.{FoldingBuilderEx, FoldingDescriptor}
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement

class LSPFoldingBuilder extends FoldingBuilderEx {
  override def buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array[FoldingDescriptor] = {
    EditorEventManager.forUri(FileUtils.documentToUri(document)).map(m => {
      m.getFoldingRanges
    }).getOrElse(Array.empty)
  }

  override def isCollapsedByDefault(node: ASTNode): Boolean = false

  override def getPlaceholderText(node: ASTNode): String = "..."
}
