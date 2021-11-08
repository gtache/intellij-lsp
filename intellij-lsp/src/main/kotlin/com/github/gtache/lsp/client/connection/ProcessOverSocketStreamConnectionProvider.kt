/* Adapted from lsp4e */
package com.github.gtache.lsp.client.connection

import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

/**
 * A connection to a process using sockets
 *
 * @param commands
 * @param workingDir
 * @param port
 */
class ProcessOverSocketStreamConnectionProvider(commands: Array<String>, workingDir: String, private val port: Int = 0) :
    ProcessStreamConnectionProvider(commands, workingDir) {

    companion object {
        private val logger = Logger.getInstance(ProcessOverSocketStreamConnectionProvider::class.java)
    }

    override var inputStream: InputStream? = null
    override var outputStream: OutputStream? = null
    override val errorStream: InputStream?
        get() = inputStream

    private var socket: Socket? = null

    override fun start(): Unit {
        val serverSocket = ServerSocket(port)
        val socketThread = thread {
            try {
                socket = serverSocket.accept()
            } catch (e: IOException) {
                logger.error(e)
            } finally {
                try {
                    serverSocket.close()
                } catch (e: IOException) {
                    logger.error(e)
                }
            }
        }
        socketThread.start()
        super.start()
        try {
            socketThread.join(5000)
        } catch (e: InterruptedException) {
            logger.error(e)
        }
        if (socket == null) throw IOException("Unable to make socket connection: " + toString()) //$NON-NLS-1$
        inputStream = socket?.getInputStream()
        outputStream = socket?.getOutputStream()
    }

    override fun stop(): Unit {
        super.stop()
        try {
            socket?.close()
        } catch (e: IOException) {
            logger.error(e)
        }
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is ProcessOverSocketStreamConnectionProvider && port == other.port
    }

    override fun hashCode(): Int {
        val result = super.hashCode()
        return Objects.hash(result, this.port)
    }
}