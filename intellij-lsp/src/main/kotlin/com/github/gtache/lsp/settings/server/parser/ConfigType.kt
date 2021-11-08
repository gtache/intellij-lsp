package com.github.gtache.lsp.settings.server.parser

/**
 * Configuration type
 */
enum class ConfigType {
    FLAT, JSON, XML;

    companion object {
        /**
         * Returns a type given an [extension]
         */
        fun fromExtension(extension: String?): ConfigType? {
            return if ("txt".equals(extension, ignoreCase = true) || "ini".equals(extension, ignoreCase = true)) {
                FLAT
            } else if (JSON.name.equals(extension, ignoreCase = true)) {
                JSON
            } else if (XML.name.equals(extension, ignoreCase = true)) {
                XML
            } else null
        }

        /**
         * Returns an extension given a [type]
         */
        fun toExtension(type: ConfigType): String? {
            return when (type) {
                FLAT -> "txt"
                JSON -> JSON.name.lowercase()
                XML -> XML.name.lowercase()
                else -> null
            }
        }
    }
}