package com.github.gtache.lsp.contributors

import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.utils.DocumentUtils
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.diagnostic.Logger

/**
 * The completion contributor for the LSP
 */
class LSPCompletionContributor : CompletionContributor() {
    companion object {
        private val logger: Logger = Logger.getInstance(LSPCompletionContributor::class.java)
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet): Unit {
        val editor = parameters.editor
        val offset = parameters.offset
        val serverPos = DocumentUtils.offsetToLSPPos(editor, offset)
        val toAdd = EditorEventManager.forEditor(editor)?.completion(serverPos) ?: emptyList()
        result.addAllElements(toAdd)
        super.fillCompletionVariants(parameters, result)
    }
}