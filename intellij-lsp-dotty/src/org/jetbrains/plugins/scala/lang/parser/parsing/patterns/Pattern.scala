package org.jetbrains.plugins.scala.lang.parser.parsing.patterns

import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder

/**
* @author Alexander Podkhalyuzin
* Date: 28.02.2008
*/

/*
 * Patern ::= Pattern1 {'|' Pattern1}
 */
object Pattern extends Pattern {
  override protected def pattern1 = Pattern1
}

trait Pattern {
  protected def pattern1: Pattern1

  def parse(builder: ScalaPsiBuilder): Boolean = {
    val patternMarker = builder.mark
    if (!pattern1.parse(builder)) {
      patternMarker.drop()
      return false
    }
    var isComposite = false
    while (builder.getTokenText == "|") {
      isComposite = true
      builder.advanceLexer() //Ate |
      if (!pattern1.parse(builder)) {
        builder error ScalaBundle.message("wrong.pattern")
      }
    }
    if (isComposite) patternMarker.done(ScalaElementTypes.PATTERN)
    else patternMarker.drop()
    true
  }
}