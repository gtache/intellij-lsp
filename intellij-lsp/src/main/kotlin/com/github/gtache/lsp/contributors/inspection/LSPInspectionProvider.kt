package com.github.gtache.lsp.contributors.inspection

import com.intellij.codeInspection.InspectionToolProvider
import com.intellij.codeInspection.LocalInspectionTool

/**
 * The provider for the LSP Inspection
 * Returns a single class, LSPInspection
 */
class LSPInspectionProvider : InspectionToolProvider {

    override fun getInspectionClasses(): Array<Class<out LocalInspectionTool>> {
        return arrayOf(LSPInspection::class.java)
    }
}