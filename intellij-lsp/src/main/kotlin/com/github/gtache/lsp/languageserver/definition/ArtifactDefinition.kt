package com.github.gtache.lsp.languageserver.definition

import com.github.gtache.lsp.client.connection.ProcessStreamConnectionProvider
import com.github.gtache.lsp.client.connection.StreamConnectionProvider
import com.github.gtache.lsp.utils.CSVLine
import com.github.gtache.lsp.utils.aether.Aether
import com.github.gtache.lsp.utils.aether.AetherException
import com.intellij.openapi.components.service

/**
 * Represents a ServerDefinition for a LanguageServer stored on a repository
 *
 * @param packge    The artifact id of the server
 * @param mainClass The main class of the server
 * @param args      The arguments to give to the main class
 */
data class ArtifactDefinition(
    override val id: String,
    private val exts: Collection<String>,
    val packge: String,
    val mainClass: String,
    val args: List<String>
) : AbstractDefinition(id, exts) {

    companion object : DefinitionObject {

        override val type: String = "artifact"

        @JvmStatic
        override val presentableType: String = "Artifact"

        override fun fromMap(map: Map<DefinitionKey, CSVLine>): ArtifactDefinition? {
            if (map[CommonDefinitionKey.TYPE]?.toSingleString() == type) {
                return ArtifactDefinition(
                    map[CommonDefinitionKey.ID]?.toSingleString() ?: return null,
                    map[CommonDefinitionKey.EXT]?.toList() ?: return null,
                    map[Key.PACKAGE]?.toSingleString() ?: return null,
                    map[Key.MAIN_CLASS]?.toSingleString() ?: return null,
                    map[Key.ARGS]?.toList() ?: return null
                )
            } else {
                throw IllegalArgumentException("Not right type : ${map[CommonDefinitionKey.TYPE]}")
            }
        }
    }

    override fun createConnectionProvider(directory: String): StreamConnectionProvider {
        val classpath = service<Aether>().resolveClasspath(packge)
        if (classpath != null) {
            val argList = mutableListOf("java", "-cp", classpath, mainClass)
            argList.addAll(args)
            return ProcessStreamConnectionProvider(argList, directory)
        } else {
            throw AetherException("Couldn't resolve artifact $packge")
        }
    }

    override fun toMap(): Map<DefinitionKey, CSVLine> {
        return super.toMap().plus(
            listOf(
                CommonDefinitionKey.TYPE to CSVLine.of(type),
                Key.PACKAGE to CSVLine.of(packge),
                Key.MAIN_CLASS to CSVLine.of(mainClass),
                Key.ARGS to CSVLine(args)
            )
        )
    }

    /**
     * Keys specific to Artifact
     */
    enum class Key : DefinitionKey {
        PACKAGE, MAIN_CLASS, ARGS
    }
}