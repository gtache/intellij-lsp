package com.github.gtache.requests

import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.jsonrpc.validation.NonNull

/**
  * Object used to process Hover responses
  */
object HoverHandler {

  /**
    * Returns the hover string corresponding to an Hover response
    *
    * @param hover The Hover
    * @return The string response
    */
  def getHoverString(@NonNull hover: Hover): String = {
    import scala.collection.JavaConverters._
    val contents = hover.getContents.asScala
    if (contents == null || contents.isEmpty) null else {
      contents.map(c => {
        if (c.isLeft) c.getLeft else if (c.isRight) {
          val markedString = c.getRight
          if (markedString.getLanguage != null && !markedString.getLanguage.isEmpty)
            s"""```${markedString.getLanguage}
${markedString.getValue}
```""" else markedString.getValue
        } else ""
      }).filter(s => !s.isEmpty).reduce((a, b) => a + "\n\n" + b)
    }
  }

}
