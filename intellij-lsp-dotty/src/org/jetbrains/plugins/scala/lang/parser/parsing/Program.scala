package org.jetbrains.plugins.scala.lang.parser.parsing

import org.jetbrains.plugins.scala.lang.parser.ErrMsg
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder

/*
 * Program parses all content in scala source file
 */
object Program extends Program {
  override protected def compilationUnit = CompilationUnit
}

trait Program {
  protected def compilationUnit: CompilationUnit

  def parse(builder: ScalaPsiBuilder): Int = {
    var parseState = 0

    if ( !builder.eof() ){
      parseState = compilationUnit.parse(builder)
    }

    if (!builder.eof()) {
      while (!builder.eof()) {
        builder error ErrMsg("out.of.compilation.unit")
        builder.advanceLexer()
      }
    }

    return parseState

  }
}