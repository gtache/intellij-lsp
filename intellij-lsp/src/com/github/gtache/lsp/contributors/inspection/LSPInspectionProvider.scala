package com.github.gtache.lsp.contributors.inspection

import com.intellij.codeInspection.InspectionToolProvider

class LSPInspectionProvider extends InspectionToolProvider {
  override def getInspectionClasses: Array[Class[_]] = {
    Array(classOf[LSPInspection])
  }
}
