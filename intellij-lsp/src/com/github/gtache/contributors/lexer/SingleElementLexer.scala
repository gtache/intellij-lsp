package com.github.gtache.contributors.lexer

import com.intellij.lexer.{Lexer, LexerPosition}
import com.intellij.psi.tree.IElementType

class SingleElementLexer(tokenType: IElementType) extends Lexer {

  private var buffer: CharSequence = ""
  private var endOffset: Int = 0
  private var offset: Int = 0
  private var state: Int = 0

  override def restore(position: LexerPosition): Unit = offset = position.getOffset

  override def start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int): Unit = {
    this.buffer = buffer
    this.endOffset = endOffset
    this.offset = startOffset
    this.state = initialState
  }

  override def getCurrentPosition: LexerPosition = LSPLexerPosition(offset, state)

  override def advance(): Unit = offset = math.min(endOffset, offset + 1)

  override def getBufferEnd: Int = endOffset

  override def getBufferSequence: CharSequence = buffer

  override def getState: Int = state

  override def getTokenEnd: Int = offset + 1

  override def getTokenStart: Int = offset

  override def getTokenType: IElementType = if (offset < endOffset) SingleElementToken else null
}
