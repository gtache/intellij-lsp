package com.github.gtache.lsp.contributors

import com.github.gtache.lsp.editor.services.application.EditorApplicationService
import com.github.gtache.lsp.utils.DocumentUtils
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.components.service

/**
 * LSP completion contributor
 */
class LSPCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet): Unit {
        val editor = parameters.editor
        val offset = parameters.offset
        val serverPos = DocumentUtils.offsetToLSPPosition(editor, offset)
        val toAdd = service<EditorApplicationService>().managerForEditor(editor)?.completion(serverPos) ?: emptyList()
        result.addAllElements(toAdd)
        super.fillCompletionVariants(parameters, result)
    }
}