package com.github.gtache.lsp.requests

/**
  * An object containing the Timeout for the various requests
  */
object Timeout {
  val SIGNATURE_TIMEOUT: Int = 1000
  val SYMBOLS_TIMEOUT: Int = 2000
  val HOVER_TIMEOUT: Int = 1000
  val COMPLETION_TIMEOUT: Int = 1000
  val DOC_HIGHLIGHT_TIMEOUT: Int = 1000
  val REFERENCES_TIMEOUT: Int = 2000
  val SHUTDOWN_TIMEOUT: Int = 5000
  val DEFINITION_TIMEOUT: Int = 2000
  val WILLSAVE_TIMEOUT: Int = 2000
  val FORMATTING_TIMEOUT: Int = 2000
  val CODEACTION_TIMEOUT: Int = 2000
  val IMMEDIATELY: Int = 0

}
