package com.github.gtache.lsp.utils

import com.intellij.openapi.diagnostic.Logger
import java.util.*

/**
 * Object containing some useful methods for the plugin
 */
object Utils {

    @JvmStatic
    val bundle: ResourceBundle = ResourceBundle.getBundle("com.github.gtache.lsp.LSPBundle")

    @JvmStatic
    val lineSeparator: String = System.getProperty("line.separator")

    private val logger: Logger = Logger.getInstance(Utils::class.java)

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