package com.github.gtache.lsp.languageserver.definition

import com.github.gtache.lsp.client.connection.StreamConnectionProvider
import com.github.gtache.lsp.utils.CSVLine

/**
 * Base class for server definitions
 */
abstract class AbstractDefinition(override val id: String, exts: Collection<String>) : Definition {

    override val extensions: MutableSet<String> = exts.toMutableSet()
        get() = field.toMutableSet()

    override val streamConnectionProvider: MutableMap<String, StreamConnectionProvider> = HashMap()


    override fun addExtension(extension: String) {
        extensions.add(extension)
    }

    override fun removeExtension(extension: String) {
        extensions.remove(extension)
    }

    override fun toMap(): Map<DefinitionKey, CSVLine> {
        return mapOf(
            CommonDefinitionKey.ID to CSVLine.of(id),
            CommonDefinitionKey.EXT to CSVLine(extensions)
        )
    }

    companion object {
        /**
         * Parse the given arguments
         */
        fun parseArgs(strArr: List<String>): List<String> {
            val list = ArrayList<String>()
            var isSingleQuote = false
            var isDoubleQuote = false
            var wasEscaped = false
            val curStr = StringBuilder()
            strArr.forEach { str ->
                for (i in str.indices) {
                    when (str[i]) {
                        '\'' -> {
                            if (!wasEscaped) {
                                isSingleQuote = !isSingleQuote
                            }
                            wasEscaped = false
                            curStr.append('\'')
                        }
                        '\"' -> {
                            if (!wasEscaped) {
                                isDoubleQuote = !isDoubleQuote
                            }
                            wasEscaped = false
                            curStr.append('\"')
                        }
                        ' ' -> {
                            if (isSingleQuote || isDoubleQuote) {
                                curStr.append(" ")
                            } else {
                                list.add(curStr.toString())
                                curStr.clear()
                            }
                            wasEscaped = false
                        }
                        '\\' -> {
                            wasEscaped = !wasEscaped
                            curStr.append('\\')
                        }
                        else -> {
                            curStr.append(str[i])
                            wasEscaped = false
                        }
                    }
                }
                if (curStr.isNotEmpty()) {
                    list.add(curStr.toString())
                    curStr.clear()
                }
            }
            return list
        }
    }
}