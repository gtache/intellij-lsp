package com.github.gtache.contributors.lexer

import com.intellij.lexer.LexerPosition

case class LSPLexerPosition(offset: Int, state: Int) extends LexerPosition {
  override def getOffset: Int = offset

  override def getState: Int = state
}
