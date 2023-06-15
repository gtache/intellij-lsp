package com.github.gtache.lsp.languageserver.definition.provider

import com.github.gtache.lsp.languageserver.definition.*
import com.github.gtache.lsp.utils.CSVLine

/**
 * Implementation of Language Server Definition Provider
 */
class LanguageServerDefinitionProviderImpl : LanguageServerDefinitionProvider {
    override fun getLanguageServerDefinition(map: Map<String, CSVLine>): Definition? {
        val clazz = when (map[CommonDefinitionKey.TYPE.name]?.toSingleString()) {
            ArtifactDefinition.type -> ArtifactDefinition.Key::class.java
            ExeDefinition.type -> ExeDefinition.Key::class.java
            RawCommandDefinition.type -> RawCommandDefinition.Key::class.java
            else -> return null
        }
        return getServerDefinition(map.mapKeys { (key) -> valueOfOrElse(key.uppercase(), clazz) })
    }

    override fun getLanguageServerDefinition(list: List<CSVLine>): Definition? {
        return getServerDefinition(getServerMap(list))
    }

    private fun valueOfOrElse(s: String, c: Class<*>): DefinitionKey {
        return try {
            CommonDefinitionKey.valueOf(s)
        } catch (e: java.lang.IllegalArgumentException) {
            if (!c.isEnum) {
                throw IllegalArgumentException("Not an enum $c")
            } else {
                (c.enumConstants as Array<Enum<*>>).first { it.name == s } as DefinitionKey
            }
        }
    }

    private fun getServerDefinition(map: Map<DefinitionKey, CSVLine>): Definition? {
        return when (map[CommonDefinitionKey.TYPE]?.toSingleString()) {
            ArtifactDefinition.type -> ArtifactDefinition.fromMap(map)
            ExeDefinition.type -> ExeDefinition.fromMap(map)
            RawCommandDefinition.type -> RawCommandDefinition.fromMap(map)
            else -> throw IllegalArgumentException("Unknown or null type : ${map[CommonDefinitionKey.TYPE]}")
        }
    }

    private fun getServerMap(list: List<CSVLine>): Map<DefinitionKey, CSVLine> {
        if (list.isNotEmpty()) {
            return when (list[0].toSingleString()) {
                ArtifactDefinition.type -> getArtifactServerMap(list)
                ExeDefinition.type -> getExeServerMap(list)
                RawCommandDefinition.type -> getRawServerMap(list)
                else -> throw IllegalArgumentException("Unknown type : $list")
            }
        } else {
            throw IllegalArgumentException("Empty list")
        }
    }

    private fun getArtifactServerMap(list: List<CSVLine>): Map<DefinitionKey, CSVLine> {
        return mapOf(
            CommonDefinitionKey.TYPE to list[0],
            CommonDefinitionKey.ID to list[1],
            ArtifactDefinition.Key.PACKAGE to list[2],
            ArtifactDefinition.Key.MAIN_CLASS to list[3],
            ArtifactDefinition.Key.ARGS to list[4]
        )
    }

    private fun getExeServerMap(list: List<CSVLine>): Map<DefinitionKey, CSVLine> {
        return mapOf(
            CommonDefinitionKey.TYPE to list[0],
            CommonDefinitionKey.ID to list[1],
            ExeDefinition.Key.PATH to list[2],
            ExeDefinition.Key.ARGS to list[3]
        )
    }

    private fun getRawServerMap(list: List<CSVLine>): Map<DefinitionKey, CSVLine> {
        return mapOf(
            CommonDefinitionKey.TYPE to list[0],
            CommonDefinitionKey.ID to list[1],
            RawCommandDefinition.Key.COMMAND to list[2]
        )
    }
}