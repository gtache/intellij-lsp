package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.head
import com.github.gtache.lsp.tail
import com.github.gtache.lsp.utils.Utils
import java.util.*


/**
 * A class representing a raw command to launch a languageserver
 *
 * @param ext     The extension
 * @param command The command to run
 */
data class RawCommandServerDefinition(override val ext: String, override val command: Array<String>) :
    BaseServerDefinition(), CommandServerDefinition {


    companion object : UserConfigurableServerDefinitionObject {

        override val typ = "rawCommand"

        @JvmStatic
        override val presentableTyp = "Raw command"

        /**
         * Transforms an array of string into the corresponding UserConfigurableServerDefinition
         *
         * @param arr The array
         * @return The server definition
         */
        override fun fromArray(arr: Array<String>): CommandServerDefinition? {
            return if (arr.head == typ) {
                val arrTail = arr.tail
                if (arrTail.size > 1) {
                    RawCommandServerDefinition(arrTail.head, Utils.parseArgs(arrTail.tail))
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    override fun toArray(): Array<String> {
        val tmp = mutableListOf(typ, ext)
        tmp.addAll(command)
        return tmp.toTypedArray()
    }

    override fun toString(): String = typ + " : " + command.joinToString(" ")

    override fun equals(other: Any?): Boolean = other is RawCommandServerDefinition && ext == other.ext &&
            command.contentEquals(other.command)

    override fun hashCode(): Int = Objects.hash(ext, command)

}