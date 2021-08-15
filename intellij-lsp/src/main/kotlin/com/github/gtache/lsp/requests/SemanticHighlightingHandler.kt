package com.github.gtache.lsp.requests

import com.github.gtache.lsp.editor.EditorProjectService
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.SemanticHighlightingParams
import java.awt.Color
import java.awt.Font

object SemanticHighlightingHandler {

    private val logger: Logger = Logger.getInstance(SemanticHighlightingHandler::class.java)
    private val mapping: Map<String, Style?>

    init {
        mapping = mutableMapOf(
            Pair("comment", Style(DefaultLanguageHighlighterColors.LINE_COMMENT)),
            Pair("comment.line", Style(DefaultLanguageHighlighterColors.LINE_COMMENT)),
            Pair("comment.line.double-slash", Style(DefaultLanguageHighlighterColors.LINE_COMMENT)),
            Pair("comment.line.double-dash", Style(DefaultLanguageHighlighterColors.LINE_COMMENT)),
            Pair("comment.line.number-sign", Style(DefaultLanguageHighlighterColors.LINE_COMMENT)),
            Pair("comment.line.percentage", Style(DefaultLanguageHighlighterColors.LINE_COMMENT)),
            Pair("comment.block", Style(DefaultLanguageHighlighterColors.BLOCK_COMMENT)),
            Pair("comment.block.documentation", Style(DefaultLanguageHighlighterColors.DOC_COMMENT)),

            Pair("constant.numeric", Style(DefaultLanguageHighlighterColors.NUMBER)),
            Pair("constant.character", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("constant.character.escape", Style(DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)),
            Pair("constant.language", Style(DefaultLanguageHighlighterColors.KEYWORD)),
            Pair("constant.other", Style(DefaultLanguageHighlighterColors.KEYWORD)),

            Pair("entity", null),
            Pair("entity.name", null),
            Pair("entity.name.function", null),
            Pair("entity.name.type", null),
            Pair("entity.name.tag", null),
            Pair("entity.name.section", null),
            Pair("entity.other", null),
            Pair("entity.other.inherited-class", null),
            Pair("entity.other.attribute-name", null),

            Pair("invalid", Style(HighlighterColors.BAD_CHARACTER)),
            Pair("invalid.illegal", Style(HighlighterColors.BAD_CHARACTER)),
            Pair("invalid.deprecated", null),

            Pair("keyword", Style(DefaultLanguageHighlighterColors.KEYWORD)),
            Pair("keyword.control", Style(DefaultLanguageHighlighterColors.KEYWORD)),
            Pair("keyword.operator", Style(DefaultLanguageHighlighterColors.KEYWORD)),
            Pair("keyword.other", Style(DefaultLanguageHighlighterColors.KEYWORD)),

            Pair("markup", null),
            Pair("markup.underline", null),
            Pair("markup.underline.link", null),
            Pair("markup.bold", Style(bold = true)),
            Pair("markup.heading", null),
            Pair("markup.italic", Style(italic = true)),
            Pair("markup.list", null),
            Pair("markup.list.numbered", null),
            Pair("markup.list.unnumbered", null),
            Pair("markup.quote", null),
            Pair("markup.raw", Style(HighlighterColors.NO_HIGHLIGHTING)),
            Pair("markup.other", null),

            Pair("meta", null),

            Pair("storage", Style(DefaultLanguageHighlighterColors.KEYWORD)),
            Pair("storage.type", Style(DefaultLanguageHighlighterColors.KEYWORD)),
            Pair("storage.modifier", Style(DefaultLanguageHighlighterColors.KEYWORD)),

            Pair("string", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("string.quoted", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("string.quoted.single", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("string.quoted.double", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("string.quoted.triple", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("string.quoted.other", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("string.unquoted", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("string.interpolated", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("string.regexp", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("string.other", Style(DefaultLanguageHighlighterColors.STRING)),

            Pair("support", Style(DefaultLanguageHighlighterColors.STRING)),
            Pair("support.function", Style(DefaultLanguageHighlighterColors.FUNCTION_CALL)),
            Pair("support.class", Style(DefaultLanguageHighlighterColors.CLASS_REFERENCE)),
            Pair("support.type", Style(DefaultLanguageHighlighterColors.CLASS_REFERENCE)),
            Pair("support.constant", Style(DefaultLanguageHighlighterColors.CONSTANT)),
            Pair("support.variable", Style(DefaultLanguageHighlighterColors.GLOBAL_VARIABLE)),
            Pair("support.other", null),

            Pair("variable", Style(DefaultLanguageHighlighterColors.GLOBAL_VARIABLE)),
            Pair("variable.parameter", Style(DefaultLanguageHighlighterColors.PARAMETER)),
            Pair("variable.language", Style(DefaultLanguageHighlighterColors.KEYWORD)),
            Pair("variable.other", Style(DefaultLanguageHighlighterColors.LOCAL_VARIABLE))
        )
    }

    fun handlePush(params: SemanticHighlightingParams, project: Project): Unit {
        val doc = params.textDocument
        val lines = params.lines
        if (doc != null && doc.uri != null && lines != null && lines.isNotEmpty()) {
            project.service<EditorProjectService>().forUri(FileUtils.sanitizeURI(doc.uri))?.semanticHighlighting(lines)
        } else logger.warn("Null semanticHighlighting identifier or lines : $doc ; $lines")
    }

    data class Style(
        private val textAttributesKey: TextAttributesKey? = null,
        private val color: Color? = null,
        private val bold: Boolean = false,
        private val italic: Boolean = false
    ) {
        fun toTextAttributes(editor: Editor): TextAttributes {
            return if (textAttributesKey != null) {
                editor.colorsScheme.getAttributes(textAttributesKey)
            } else {
                val fontType = if (bold) if (italic) Font.BOLD or Font.ITALIC else Font.BOLD else if (italic) Font.ITALIC else Font.PLAIN
                TextAttributes(color, null, null, null, fontType)
            }
        }
    }

    fun scopeToStyle(scope: String): Style? {
        //TODO proper
        return if (scope.split(".").size > 3) {
            mapping[scope.split(".").take(3).joinToString(".")]
        } else {
            mapping[scope]
        }
    }

    fun scopeToTextAttributes(scope: String, editor: Editor): TextAttributes? {
        return mapping[scope]?.toTextAttributes(editor)
    }

}