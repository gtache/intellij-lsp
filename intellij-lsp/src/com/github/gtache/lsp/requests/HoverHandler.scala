package com.github.gtache.lsp.requests

import com.intellij.openapi.diagnostic.Logger
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.jsonrpc.validation.NonNull

import scala.collection.mutable.ArrayBuffer

/**
  * Object used to process Hover responses
  */
object HoverHandler {

  val MARKUP_PLAIN: String = "plaintext";
  val MARKUP_MARKUP: String = "markup"

  private val LOG: Logger = Logger.getInstance(HoverHandler.getClass)

  /**
    * Returns the hover string corresponding to an Hover response
    *
    * @param hover The Hover
    * @return The string response
    */
  def getHoverString(@NonNull hover: Hover): String = {
    import scala.collection.JavaConverters._
    if (hover != null && hover.getContents != null) {
      val hoverContents = hover.getContents
      if (hoverContents.isLeft) {
        val contents = hoverContents.getLeft.asScala
        val result = if (contents == null || contents.isEmpty) "" else {
          val parsedContent = contents.map(c => {
            if (c.isLeft) parsePlain(c.getLeft) else if (c.isRight) {
              val markedString = c.getRight
              val string = if (markedString.getLanguage != null && !markedString.getLanguage.isEmpty)
                s"""```${markedString.getLanguage}${markedString.getValue}```""" else markedString.getValue
              parseMarkup(string)
            } else ""
          }).filter(s => !s.isEmpty)
          if (parsedContent.isEmpty) {
            ""
          } else {
            parsedContent.reduce((a, b) => a + "\n\n" + b)
          }
        }
        "<html>" + result + "</html>"
      } else if (hoverContents.isRight) {
        val markupContent = hoverContents.getRight
        val value = markupContent.getValue
        val result = markupContent.getKind match {
          case MARKUP_MARKUP => parseMarkup(value)
          case MARKUP_PLAIN => "<html>" + parsePlain(value) + "</html>"
          case _ => "<html>" + parsePlain(value) + "</html>"
        }
        result
      } else ""
    } else ""
  }

  private def parseMarkup(markup: String): String = {
    val options = new MutableDataSet()
    val parser = Parser.builder(options).build()
    val renderer = HtmlRenderer.builder(options).build()
    if (markup.nonEmpty) renderer.render(parser.parse(markup)) else ""
  }

  private def parsePlain(text: String): String = {
    val arr = text.split("\n")
    arr.flatMap(s => {
      val sentences = s.split("(?<=[^.]{2})\\.(?=[^.])").map(s => if (s.nonEmpty) s + "." else s)
      var count = 0
      val curBlock: StringBuilder = new StringBuilder
      val result = ArrayBuffer[String]()
      sentences.indices.foreach(idx => {
        val sentence = sentences(idx)
        curBlock ++= sentence
        count += sentence.length
        //TODO something smarter
        if (count > 100) {
          result.append(curBlock.toString)
          curBlock.clear()
          count = 0
        }
      })
      if (curBlock.nonEmpty) result.append(curBlock.toString)
      result
    }).mkString("<br />")
  }
}
