package com.github.gtache.lsp.requests

import com.intellij.openapi.diagnostic.Logger
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.jsonrpc.validation.NonNull

/**
 * Object used to process Hover responses
 */
object HoverHandler {

    const val MARKUP_PLAIN: String = "plaintext"
    const val MARKUP_MARKUP: String = "markup"

    private val logger: Logger = Logger.getInstance(HoverHandler::class.java)

    /**
     * Returns the hover string corresponding to an Hover response
     *
     * @param hover The Hover
     * @return The string response
     */
    fun getHoverString(@NonNull hover: Hover): String {
        if (hover.contents != null) {
            val hoverContents = hover.contents
            if (hoverContents.isLeft) {
                val contents = hoverContents.left
                val result = if (contents == null || contents.isEmpty()) "" else {
                    val parsedContent = contents.map { c ->
                        if (c.isLeft) parsePlain(c.left) else if (c.isRight) {
                            val markedString = c.right
                            val string = if (markedString.language != null && markedString.language.isNotEmpty())
                                "```${markedString.language}${markedString.value}```" else markedString.value
                            parseMarkup(string)
                        } else ""
                    }.filter { s -> s.isNotEmpty() }
                    if (parsedContent.isEmpty()) {
                        ""
                    } else {
                        parsedContent.reduce { a, b -> a + "\n\n" + b }
                    }
                }
                return "<html>$result</html>"
            } else if (hoverContents.isRight) {
                val markupContent = hoverContents.right
                val value = markupContent.value
                val result = when (markupContent.kind) {
                    MARKUP_MARKUP -> parseMarkup(value)
                    MARKUP_PLAIN -> "<html>" + parsePlain(value) + "</html>"
                    else -> "<html>" + parsePlain(value) + "</html>"
                }
                return result
            } else return ""
        } else return ""
    }

    private fun parseMarkup(markup: String): String {
        val options = MutableDataSet()
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        return if (markup.isNotEmpty()) renderer.render(parser.parse(markup)) else ""
    }

    private fun parsePlain(text: String): String {
        val arr = text.split("\n")
        return arr.flatMap { s ->
            val sentences = s.split("(?<=<^.>{2})\\.(?=<^.>)").map { sub -> if (sub.isNotEmpty()) "$sub." else sub }
            var count = 0
            val curBlock = StringBuilder()
            val result = ArrayList<String>()
            sentences.indices.forEach { idx ->
                val sentence = sentences[idx]
                curBlock.append(sentence)
                count += sentence.length
                //TODO something smarter
                if (count > 100) {
                    result.add(curBlock.toString())
                    curBlock.clear()
                    count = 0
                }
            }
            if (curBlock.isNotEmpty()) result.add(curBlock.toString())
            result
        }.joinToString("<br />")
    }
}