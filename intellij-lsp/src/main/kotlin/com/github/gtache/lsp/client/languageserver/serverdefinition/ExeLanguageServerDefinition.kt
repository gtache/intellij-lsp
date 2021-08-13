package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.head
import com.github.gtache.lsp.tail
import com.github.gtache.lsp.utils.Utils
import com.intellij.openapi.diagnostic.Logger
import java.util.*

/**
 * Class representing server definitions corresponding to an executable file
 * This class is basically a more convenient way to write a RawCommand
 *
 * @param ext  The extension
 * @param path The path to the exe file
 * @param args The arguments for the exe file
 */
data class ExeLanguageServerDefinition(
    override val ext: String,
    val path: String,
    val args: Array<String>
) : BaseServerDefinition(), CommandServerDefinition {

    companion object : UserConfigurableServerDefinitionObject {
        private val logger: Logger = Logger.getInstance(UserConfigurableServerDefinitionObject::class.java)

        override val typ: String = "exe"

        @JvmStatic
        override val presentableTyp: String = "Executable"

        override fun fromArray(arr: Array<String>): ExeLanguageServerDefinition? {
            if (arr.head == typ) {
                val arrTail = arr.tail
                return if (arrTail.size < 2) {
                    logger.warn("Not enough elements to translate into a ServerDefinition : " + arr.joinToString(" ; "))
                    null
                } else {
                    //TODO for cquery, remove
                    ExeLanguageServerDefinition(
                        arrTail.head,
                        arrTail.tail.head,
                        if (arrTail.size > 2) Utils.parseArgs(arrTail.drop(2)) else emptyArray()
                    )
                }
            } else {
                return null
            }
        }
    }

    override val command: Array<String>

    init {
        val tmp = mutableListOf(path)
        tmp.addAll(args)
        command = tmp.toTypedArray()
    }

    override fun toArray(): Array<String> {
        val tmp = mutableListOf(typ, ext, path)
        tmp.addAll(args)
        return tmp.toTypedArray()
    }

    override fun toString(): String = typ + " : path " + path + " args : " + args.joinToString(" ")


    override fun equals(other: Any?): Boolean = other is ExeLanguageServerDefinition && ext == other.ext && path == other.path &&
            args.contentEquals(other.args)

    override fun hashCode(): Int = Objects.hash(ext, path, args)

}