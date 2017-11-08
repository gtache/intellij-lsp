package com.github.gtache.lsp.contributors

import com.github.gtache.lsp.PluginMain
import com.github.gtache.lsp.utils.Utils
import com.intellij.codeInsight.completion.{CompletionContributor, CompletionParameters, CompletionResultSet}
import com.intellij.openapi.diagnostic.Logger

/**
  * The completion contributor for the LSP
  */
class LSPCompletionContributor extends CompletionContributor {
  private val LOG: Logger = Logger.getInstance(this.getClass)

  override def fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet): Unit = {
    val editor = parameters.getEditor
    val offset = parameters.getOffset
    val serverPos = Utils.logicalToLSPPos(editor.offsetToLogicalPosition(offset))
    val toAdd = PluginMain.completion(editor, serverPos)
    result.addAllElements(toAdd)
    super.fillCompletionVariants(parameters, result)
  }
}
