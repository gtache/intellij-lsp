package com.github.gtache.requests

import com.github.gtache.{PluginMain, Utils}
import com.intellij.codeInsight.completion.{CompletionContributor, CompletionParameters, CompletionResultSet}

/**
  * The completion contributor for the LSP
  */
class CompletionHandler extends CompletionContributor {

  override def fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet): Unit = {
    //TODO Need filetypes
    val editor = parameters.getEditor
    val offset = parameters.getOffset
    val serverPos = Utils.logicalToLspPos(editor.offsetToLogicalPosition(offset))
    result.addAllElements(PluginMain.completion(editor, serverPos))

    super.fillCompletionVariants(parameters, result)
  }
}
