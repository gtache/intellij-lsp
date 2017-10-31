package com.github.gtache.editor

import com.intellij.openapi.editor.markup.RangeHighlighter

case class DiagnosticRangeHighlighter(rangeHighlighter: RangeHighlighter, message: String, source: String, code: String) {

}
