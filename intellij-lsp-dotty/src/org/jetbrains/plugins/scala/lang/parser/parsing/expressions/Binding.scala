package org.jetbrains.plugins.scala.lang.parser.parsing.expressions

import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.{ErrMsg, ScalaElementTypes}
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder
import org.jetbrains.plugins.scala.lang.parser.parsing.types.ParamType

/**
 * @author Aleksander Podkhalyuzin
 * @since 05.04.2009
 */

/**
 * Binding ::= (id | '_') [':' Type]
 */

object Binding extends Binding {
  override protected def paramType = ParamType
}

trait Binding {
  protected def paramType: ParamType

  def parse(builder: ScalaPsiBuilder): Boolean = {
    val paramMarker = builder.mark
    builder.getTokenType match {
      case ScalaTokenTypes.tIDENTIFIER | ScalaTokenTypes.tUNDER =>
        builder.mark.done(ScalaElementTypes.ANNOTATIONS)
        builder.advanceLexer()
      case _ =>
        paramMarker.drop()
        return false
    }

    builder.getTokenType match {
      case ScalaTokenTypes.tCOLON =>
        builder.advanceLexer() //Ate :
        if (!paramType.parse(builder)) builder error ErrMsg("wrong.type")
      case _ =>
    }

    paramMarker.done(ScalaElementTypes.PARAM)
    true
  }
}