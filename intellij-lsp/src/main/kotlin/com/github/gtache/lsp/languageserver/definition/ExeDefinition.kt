package com.github.gtache.lsp.languageserver.definition

import com.github.gtache.lsp.utils.CSVLine

/**
 * Class representing server definitions corresponding to an executable file
 * This class is basically a more convenient way to write a RawCommand
 *
 * @param path The path to the exe file
 * @param args The arguments for the exe file
 */
data class ExeDefinition(
    override val id: String,
    private val exts: Collection<String>,
    val path: String,
    val args: List<String>
) : AbstractDefinition(id, exts), CommandDefinition {

    companion object : DefinitionObject {
        override val type: String = "exe"

        @JvmStatic
        override val presentableType: String = "Executable"
        override fun fromMap(map: Map<DefinitionKey, CSVLine>): ExeDefinition? {
            if (map[CommonDefinitionKey.TYPE]?.toSingleString() == type) {
                return ExeDefinition(
                    map[CommonDefinitionKey.ID]?.toSingleString() ?: return null,
                    map[CommonDefinitionKey.EXT]?.toList() ?: return null,
                    map[Key.PATH]?.toSingleString() ?: return null,
                    map[Key.ARGS]?.toList() ?: return null
                )
            } else {
                throw IllegalArgumentException("Not right type : ${map[CommonDefinitionKey.TYPE]}")
            }
        }
    }

    override val command: List<String>

    override fun toMap(): Map<DefinitionKey, CSVLine> {
        return super.toMap().plus(listOf(CommonDefinitionKey.TYPE to CSVLine.of(type), Key.PATH to CSVLine.of(path), Key.ARGS to CSVLine(args)))
    }

    init {
        val tmp = mutableListOf(path)
        tmp.addAll(args)
        command = tmp
    }

    /**
     * Keys specific to Exe
     */
    enum class Key : DefinitionKey {
        PATH, ARGS
    }
}