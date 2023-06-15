package com.github.gtache.lsp.settings.project.converters

import com.github.gtache.lsp.utils.CSVLine
import com.intellij.util.xml.Converter

abstract class AbstractConverter<T> : Converter<T>() {
    companion object {
        fun wrap(vararg objs: Any): String {
            return CSVLine(objs.map { it.toString() }.toList()).csvLine
        }

        fun unwrap(s: String): List<String> {
            return CSVLine(s).toList()
        }
    }
}