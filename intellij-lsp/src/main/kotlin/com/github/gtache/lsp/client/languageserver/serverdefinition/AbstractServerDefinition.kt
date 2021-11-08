package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.client.connection.StreamConnectionProvider
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition.Companion.splitExtension

/**
 * Base class for server definitions
 */
abstract class AbstractServerDefinition : LanguageServerDefinition {
    private val _mappedExtensions: MutableSet<String> by lazy {
        splitExtension(ext).toMutableSet()
    }
    override val mappedExtensions: MutableSet<String>
        get() = HashSet(_mappedExtensions)

    override val streamConnectionProviders: MutableMap<String, StreamConnectionProvider> = HashMap()

    companion object {
        /**
         * Parse the given arguments
         */
        fun parseArgs(strArr: Iterable<String>): Array<String> {
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
            return list.toTypedArray()
        }
    }
}