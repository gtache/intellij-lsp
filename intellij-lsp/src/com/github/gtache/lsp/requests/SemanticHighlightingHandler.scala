package com.github.gtache.lsp.requests


import java.awt.{Color, Font}

import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.{DefaultLanguageHighlighterColors, Editor, HighlighterColors}
import org.eclipse.lsp4j.SemanticHighlightingParams

object SemanticHighlightingHandler {

  private val LOG: Logger = Logger.getInstance(SemanticHighlightingHandler.getClass)
  val mapping: Map[String, Style] = Map(
    "comment" -> Style(DefaultLanguageHighlighterColors.LINE_COMMENT),
    "comment.line" -> Style(DefaultLanguageHighlighterColors.LINE_COMMENT),
    "comment.line.double-slash" -> Style(DefaultLanguageHighlighterColors.LINE_COMMENT),
    "comment.line.double-dash" -> Style(DefaultLanguageHighlighterColors.LINE_COMMENT),
    "comment.line.number-sign" -> Style(DefaultLanguageHighlighterColors.LINE_COMMENT),
    "comment.line.percentage" -> Style(DefaultLanguageHighlighterColors.LINE_COMMENT),
    "comment.block" -> Style(DefaultLanguageHighlighterColors.BLOCK_COMMENT),
    "comment.block.documentation" -> Style(DefaultLanguageHighlighterColors.DOC_COMMENT),

    "constant.numeric" -> Style(DefaultLanguageHighlighterColors.NUMBER),
    "constant.character" -> Style(DefaultLanguageHighlighterColors.STRING),
    "constant.character.escape" -> Style(DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE),
    "constant.language" -> Style(DefaultLanguageHighlighterColors.KEYWORD),
    "constant.other" -> Style(DefaultLanguageHighlighterColors.KEYWORD),

    "entity" -> null,
    "entity.name" -> null,
    "entity.name.function" -> null,
    "entity.name.type" -> null,
    "entity.name.tag" -> null,
    "entity.name.section" -> null,
    "entity.other" -> null,
    "entity.other.inherited-class" -> null,
    "entity.other.attribute-name" -> null,

    "invalid" -> Style(HighlighterColors.BAD_CHARACTER),
    "invalid.illegal" -> Style(HighlighterColors.BAD_CHARACTER),
    "invalid.deprecated" -> null,

    "keyword" -> Style(DefaultLanguageHighlighterColors.KEYWORD),
    "keyword.control" -> Style(DefaultLanguageHighlighterColors.KEYWORD),
    "keyword.operator" -> Style(DefaultLanguageHighlighterColors.KEYWORD),
    "keyword.other" -> Style(DefaultLanguageHighlighterColors.KEYWORD),

    "markup" -> null,
    "markup.underline" -> null,
    "markup.underline.link" -> null,
    "markup.bold" -> Style(bold = true),
    "markup.heading" -> null,
    "markup.italic" -> Style(italic = true),
    "markup.list" -> null,
    "markup.list.numbered" -> null,
    "markup.list.unnumbered" -> null,
    "markup.quote" -> null,
    "markup.raw" -> Style(HighlighterColors.NO_HIGHLIGHTING),
    "markup.other" -> null,

    "meta" -> null,

    "storage" -> Style(DefaultLanguageHighlighterColors.KEYWORD),
    "storage.type" -> Style(DefaultLanguageHighlighterColors.KEYWORD),
    "storage.modifier" -> Style(DefaultLanguageHighlighterColors.KEYWORD),

    "string" -> Style(DefaultLanguageHighlighterColors.STRING),
    "string.quoted" -> Style(DefaultLanguageHighlighterColors.STRING),
    "string.quoted.single" -> Style(DefaultLanguageHighlighterColors.STRING),
    "string.quoted.double" -> Style(DefaultLanguageHighlighterColors.STRING),
    "string.quoted.triple" -> Style(DefaultLanguageHighlighterColors.STRING),
    "string.quoted.other" -> Style(DefaultLanguageHighlighterColors.STRING),
    "string.unquoted" -> Style(DefaultLanguageHighlighterColors.STRING),
    "string.interpolated" -> Style(DefaultLanguageHighlighterColors.STRING),
    "string.regexp" -> Style(DefaultLanguageHighlighterColors.STRING),
    "string.other" -> Style(DefaultLanguageHighlighterColors.STRING),

    "support" -> Style(DefaultLanguageHighlighterColors.STRING),
    "support.function" -> Style(DefaultLanguageHighlighterColors.FUNCTION_CALL),
    "support.class" -> Style(DefaultLanguageHighlighterColors.CLASS_REFERENCE),
    "support.type" -> Style(DefaultLanguageHighlighterColors.CLASS_REFERENCE),
    "support.constant" -> Style(DefaultLanguageHighlighterColors.CONSTANT),
    "support.variable" -> Style(DefaultLanguageHighlighterColors.GLOBAL_VARIABLE),
    "support.other" -> null,

    "variable" -> Style(DefaultLanguageHighlighterColors.GLOBAL_VARIABLE),
    "variable.parameter" -> Style(DefaultLanguageHighlighterColors.PARAMETER),
    "variable.language" -> Style(DefaultLanguageHighlighterColors.KEYWORD),
    "variable.other" -> Style(DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
  )

  def handlePush(params: SemanticHighlightingParams): Unit = {
    import scala.collection.JavaConverters._
    if (params != null) {
      val doc = params.getTextDocument
      val lines = params.getLines
      if (doc != null && doc.getUri != null && lines != null && !lines.isEmpty) {
        EditorEventManager.forUri(FileUtils.sanitizeURI(doc.getUri)).foreach(m => {
          m.semanticHighlighting(lines.asScala)
        })
      } else LOG.warn("Null semanticHighlighting identifier or lines : " + doc + " ; " + lines)
    } else LOG.warn("Null semanticHighlightingParams")
  }

  case class Style(textAttributesKey: TextAttributesKey = null, color: Color = null, bold: Boolean = false, italic: Boolean = false) {
    def toTextAttributes(editor: Editor): TextAttributes = {
      if (textAttributesKey != null) {
        editor.getColorsScheme.getAttributes(textAttributesKey)
      } else {
        val fontType = if (bold) if (italic) Font.BOLD | Font.ITALIC else Font.BOLD else if (italic) Font.ITALIC else Font.PLAIN
        new TextAttributes(color, null, null, null, fontType)
      }
    }
  }

  def scopeToStyle(scope: String) : Style = {
    ???
  }

  def scopeToTextAttributes(scope: String) : TextAttributes = {
    ???
  }

}