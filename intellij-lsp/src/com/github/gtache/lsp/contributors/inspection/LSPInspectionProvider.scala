package com.github.gtache.lsp.contributors.inspection

import com.intellij.codeInspection.{InspectionToolProvider, LocalInspectionTool}

/**
  * The provider for the LSP Inspection
  * Returns a single class, LSPInspection
  */
class LSPInspectionProvider extends InspectionToolProvider {

  override def getInspectionClasses: Array[Class[_ <: LocalInspectionTool]] = {
    Array(classOf[LSPInspection])
  }
}
