/* Adapted from lsp4e*/
package com.github.gtache.lsp.client.connection

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * A class representing a connection to a process
 *
 * @param commands   The commands to start the process
 * @param workingDir The working directory of the process
 */
open class ProcessStreamConnectionProvider(private var commands: List<String>, private var workingDir: String) :
    StreamConnectionProvider {

    companion object {
        private val logger: Logger = Logger.getInstance(ProcessStreamConnectionProvider::class.java)
    }

    private var process: Process? = null

    override val inputStream: InputStream?
        get() = process?.inputStream
    override val errorStream: InputStream?
        get() = process?.errorStream
    override val outputStream: OutputStream?
        get() = process?.outputStream

    override fun start(): Unit {
        if (this.commands.isEmpty()) {
            throw IOException("Unable to start language server: $this")
        }
        val builder = createProcessBuilder()
        logger.info("Starting server process , commands ${commands.joinToString(" ")} and workingDir $workingDir")
        this.process = builder.start()
        process?.let {
            if (!it.isAlive) throw IOException("Unable to start language server: $this") else logger.info("Server process started $process")
        }
    }

    private fun createProcessBuilder(): ProcessBuilder {
        val builder = ProcessBuilder(commands.map { s -> s.replace("\'", "") })
        builder.directory(File(workingDir))
        return builder
    }

    override fun stop(): Unit {
        process?.destroy()
    }

    override fun equals(other: Any?): Boolean {
        return other is ProcessStreamConnectionProvider && commands.size == other.commands.size &&
                this.commands == other.commands && this.workingDir == other.workingDir

    }

    override fun hashCode(): Int {
        return Objects.hash(this.commands, this.workingDir)
    }
}