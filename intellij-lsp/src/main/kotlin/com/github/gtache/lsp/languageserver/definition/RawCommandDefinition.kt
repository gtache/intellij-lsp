package com.github.gtache.lsp.languageserver.definition

import com.github.gtache.lsp.utils.CSVLine


/**
 * A class representing a raw command to launch a languageserver
 *
 * @param command The command to run
 */
data class RawCommandDefinition(override val id: String, private val exts: Collection<String>, override val command: List<String>) :
    AbstractDefinition(id, exts), CommandDefinition {


    companion object : DefinitionObject {
        override val type: String = "rawCommand"

        @JvmStatic
        override val presentableType: String = "Raw command"

        override fun fromMap(map: Map<DefinitionKey, CSVLine>): RawCommandDefinition? {
            if (map[CommonDefinitionKey.TYPE]?.toSingleString() == type) {
                return RawCommandDefinition(
                    map[CommonDefinitionKey.ID]?.toSingleString() ?: return null,
                    map[CommonDefinitionKey.EXT]?.toList() ?: return null,
                    map[Key.COMMAND]?.toList() ?: return null
                )
            } else {
                throw IllegalArgumentException("Not right type : ${map[CommonDefinitionKey.TYPE]}")
            }
        }
    }

    /**
     * Keys specific to RawCommand
     */
    enum class Key : DefinitionKey {
        COMMAND
    }

    override fun toMap(): Map<DefinitionKey, CSVLine> {
        return super.toMap().plus(
            listOf(
                CommonDefinitionKey.TYPE to CSVLine.of(type),
                Key.COMMAND to CSVLine(command)
            )
        )
    }

}