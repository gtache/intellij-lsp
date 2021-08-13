package com.github.gtache.lsp.settings.server.parser

enum class ConfigType {
    FLAT, JSON, XML;

    companion object {
        fun fromExt(ext: String?): ConfigType? {
            return if ("FLAT".equals(ext, ignoreCase = true)) {
                FLAT
            } else if ("JSON".equals(ext, ignoreCase = true)) {
                JSON
            } else if ("XML".equals(ext, ignoreCase = true)) {
                XML
            } else null
        }

        fun toExt(typ: ConfigType): String? {
            return when (typ) {
                FLAT -> "txt"
                JSON -> "json"
                XML -> "xml"
                else -> null
            }
        }
    }
}