package com.github.gtache.lsp.editor

import com.intellij.openapi.editor.markup.RangeHighlighter

/**
  * A class representing a Diagnostic Range
  * The diagnostic is sent from the server, the rangeHighlighter is created from the severity and range of the diagnostic
  *
  * @param rangeHighlighter The rangeHighlighter of the diagnostic
  * @param message          The message of the diagnostic
  * @param source           The source of the diagnostic
  * @param code             The code of the diagnostic
  */
case class DiagnosticRangeHighlighter(rangeHighlighter: RangeHighlighter, message: String, source: String, code: String) {

}
