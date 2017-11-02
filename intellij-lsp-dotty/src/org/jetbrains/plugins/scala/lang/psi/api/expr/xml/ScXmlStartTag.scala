package org.jetbrains.plugins.scala.lang.psi.api.expr.xml

/**
* @author Alexander Podkhalyuzin
* Date: 21.04.2008
*/

trait ScXmlStartTag extends ScXmlPairedTag {
  def getClosingTag: ScXmlEndTag = {
    if (getParent != null && getParent.getLastChild.isInstanceOf[ScXmlEndTag]) {
      return getParent.getLastChild.asInstanceOf[ScXmlEndTag]
    }
    null
  }

  def getMatchedTag: ScXmlPairedTag = getClosingTag
}