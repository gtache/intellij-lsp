package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.client.connection.ProcessStreamConnectionProvider
import com.github.gtache.lsp.client.connection.StreamConnectionProvider
import com.github.gtache.lsp.head
import com.github.gtache.lsp.tail
import com.github.gtache.lsp.utils.Utils
import com.github.gtache.lsp.utils.aether.Aether
import com.github.gtache.lsp.utils.coursier.AetherException
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import java.util.*

/**
 * Represents a ServerDefinition for a LanguageServer stored on a repository
 *
 * @param ext       The extension that the server manages
 * @param packge    The artifact id of the server
 * @param mainClass The main class of the server
 * @param args      The arguments to give to the main class
 */
data class ArtifactLanguageServerDefinition(
    override val ext: String,
    val packge: String,
    val mainClass: String,
    val args: Array<String>
) :
    BaseServerDefinition(), UserConfigurableServerDefinition {

    companion object : UserConfigurableServerDefinitionObject {
        private val logger: Logger = Logger.getInstance(ArtifactLanguageServerDefinition::class.java)

        override val typ: String = "artifact"

        @JvmStatic
        override val presentableTyp: String = "Artifact"

        override fun fromArray(arr: Array<String>): ArtifactLanguageServerDefinition? {
            return if (arr.head == typ) {
                val arrTail = arr.tail
                if (arrTail.size < 3) {
                    logger.warn("Not enough elements to translate into a ServerDefinition : " + arr.joinToString(" ; "))
                    null
                } else {
                    ArtifactLanguageServerDefinition(
                        arrTail.head,
                        arrTail.tail.head,
                        arrTail.drop(2).head,
                        if (arrTail.size > 3) Utils.parseArgs(arrTail.drop(3)) else emptyArray()
                    )
                }
            } else {
                null
            }
        }
    }

    override fun createConnectionProvider(workingDir: String): StreamConnectionProvider {
        val classpath = service<Aether>().resolveClasspath(packge)
        if (classpath != null) {
            val argList = mutableListOf("java", "-cp", classpath, mainClass)
            argList.addAll(args)
            return ProcessStreamConnectionProvider(argList.toTypedArray(), workingDir)
        } else {
            throw AetherException("Couldn't resolve artifact $packge")
        }
    }

    override fun toString(): String = super.toString() + " " + typ + " : " + packge + " mainClass : " + mainClass +
            " args : " + args.joinToString(" ")

    override fun toArray(): Array<String> {
        val tmp = mutableListOf(typ, ext, packge, mainClass)
        tmp.addAll(args)
        return tmp.toTypedArray()
    }

    override fun equals(other: Any?): Boolean = other is ArtifactLanguageServerDefinition && ext == other.ext && packge == other.packge && mainClass == other.mainClass && args.contentEquals(
        other.args
    )

    override fun hashCode(): Int = Objects.hash(ext, packge, mainClass, args)

}