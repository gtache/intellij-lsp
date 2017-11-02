package org.jetbrains.plugins.scala.lang.psi.api.expr.xml

/**
* @author Alexander Podkhalyuzin
* Date: 21.04.2008
*/

trait ScXmlEndTag extends ScXmlPairedTag {
  def getOpeningTag: ScXmlStartTag = {
    if (getParent != null && getParent.getFirstChild.isInstanceOf[ScXmlStartTag]) {
      return getParent.getFirstChild.asInstanceOf[ScXmlStartTag]
    }
    null
  }

  def getMatchedTag: ScXmlPairedTag = getOpeningTag
}