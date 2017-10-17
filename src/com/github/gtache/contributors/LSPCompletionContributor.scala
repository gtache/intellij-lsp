package com.github.gtache.contributors

import com.github.gtache.{PluginMain, Utils}
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
    import scala.collection.JavaConverters._
    LOG.info("Added completions " + toAdd.asScala.mkString(";"))
    result.addAllElements(toAdd)
    super.fillCompletionVariants(parameters, result)
  }
}
