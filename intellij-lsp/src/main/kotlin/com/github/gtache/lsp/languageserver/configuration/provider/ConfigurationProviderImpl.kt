package com.github.gtache.lsp.languageserver.configuration.provider

import com.github.gtache.lsp.languageserver.configuration.Configuration
import com.github.gtache.lsp.languageserver.configuration.ConfigurationImpl
import com.github.gtache.lsp.languageserver.configuration.parser.*
import java.io.File
import java.nio.charset.Charset

/**
 * Implementation of ConfigurationProvider
 */
class ConfigurationProviderImpl : ConfigurationProvider {

    override fun getConfiguration(file: File, charset: Charset): Configuration? {
        val parser = forFile(file)
        return parser?.parse(file, charset)
    }

    override fun getConfiguration(document: String, type: ConfigType): Configuration? {
        val parser = forType(type)
        return parser?.parse(document)
    }

    override fun getConfiguration(files: List<File>, charset: Charset): Configuration {
        TODO("Not yet implemented")
    }

    override fun getInvalidConfiguration(): Configuration {
        return INVALID_CONFIGURATION
    }

    override fun getEmptyConfiguration(): Configuration {
        return EMPTY_CONFIGURATION
    }

    override fun combineConfigurations(
        firstConfig: Map<String, Map<String, Any?>>,
        secondConfig: Map<String, Map<String, Any?>>
    ): Map<String, Map<String, Any?>> {
        val concatMap = HashMap<String, MutableMap<String, Any?>>()
        firstConfig.keys.forEach { key ->
            concatMap[key] = HashMap(firstConfig[key])
        }
        secondConfig.keys.forEach { key ->
            if (concatMap.contains(key)) {
                secondConfig[key]?.keys?.forEach { subkey ->
                    concatMap[key]?.put(subkey, secondConfig[key]?.get(subkey))
                }
            } else {
                concatMap[key] = HashMap(secondConfig[key])
            }
        }
        return concatMap.map { (key, value) -> key to value.toMap() }.toMap()
    }

    companion object {

        /**
         * An empty configuration
         */
        val EMPTY_CONFIGURATION: Configuration = ConfigurationImpl(mapOf(Pair("global", HashMap())))

        /**
         * An invalid configuration
         */
        val INVALID_CONFIGURATION: Configuration = ConfigurationImpl(emptyMap())

        private fun forType(typ: ConfigType?): ConfigurationParser? {
            return when (typ) {
                ConfigType.FLAT -> FlatParser()
                ConfigType.JSON -> JsonParser()
                ConfigType.XML -> XmlParser()
                else -> null
            }
        }

        private fun forFile(file: File): ConfigurationParser? {
            val name = file.name
            val idx = name.lastIndexOf('.')
            return if (idx + 1 < name.length) {
                forExt(name.drop(idx + 1))
            } else null
        }

        private fun forExt(ext: String): ConfigurationParser? {
            return forType(ConfigType.fromExtension(ext))
        }
    }
}