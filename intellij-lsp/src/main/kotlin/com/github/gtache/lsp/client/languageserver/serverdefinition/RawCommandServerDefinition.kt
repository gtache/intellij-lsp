package com.github.gtache.lsp.client.languageserver.serverdefinition

import com.github.gtache.lsp.head
import com.github.gtache.lsp.tail
import java.util.*


/**
 * A class representing a raw command to launch a languageserver
 *
 * @param ext     The extension
 * @param command The command to run
 */
data class RawCommandServerDefinition(override val ext: String, override val command: Array<String>) :
    AbstractServerDefinition(), CommandServerDefinition {


    companion object : UserConfigurableServerDefinitionObject {

        override val type: String = "rawCommand"

        @JvmStatic
        override val presentableType: String = "Raw command"

        /**
         * Transforms an array of string into the corresponding UserConfigurableServerDefinition
         *
         * @param arr The array
         * Returns The server definition
         */
        override fun fromArray(arr: Array<String>): CommandServerDefinition? {
            return if (arr.head == type) {
                val arrTail = arr.tail
                if (arrTail.size > 1) {
                    RawCommandServerDefinition(arrTail.head, parseArgs(arrTail.tail))
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    override fun toArray(): Array<String> {
        val tmp = mutableListOf(type, ext)
        tmp.addAll(command)
        return tmp.toTypedArray()
    }

    override fun toString(): String = type + " : " + command.joinToString(" ")

    override fun equals(other: Any?): Boolean = other is RawCommandServerDefinition && ext == other.ext &&
            command.contentEquals(other.command)

    override fun hashCode(): Int = Objects.hash(ext, command)

}