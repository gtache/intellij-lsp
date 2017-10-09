/** *****************************************************************************
  * Copyright (c) 2017 Rogue Wave Software Inc. and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * Michał Niewrzał (Rogue Wave Software Inc.) - initial implementation
  * ******************************************************************************/
package com.github.gtache.client

import java.io.{IOException, InputStream, OutputStream}
import java.net.{ServerSocket, Socket}
import java.util.Objects

import com.intellij.openapi.diagnostic.Logger

/**
  * Unused at the moment
  *
  * @param commands
  * @param workingDir
  * @param port
  */
class ProcessOverSocketStreamConnectionProvider(commands: Seq[String], workingDir: String, port: Int = 0) extends ProcessStreamConnectionProvider(commands, workingDir) {
  private val LOG = Logger.getInstance(classOf[ProcessStreamConnectionProvider])
  private var socket: Socket = _
  private var inputStream: InputStream = _
  private var outputStream: OutputStream = _

  @throws[IOException]
  override def start(): Unit = {
    val serverSocket = new ServerSocket(port)
    val socketThread = new Thread(() => {
      try
        socket = serverSocket.accept
      catch {
        case e: IOException =>
          LOG.error(e)
      } finally try
        serverSocket.close()
      catch {
        case e: IOException =>
          LOG.error(e)
      }
    })
    socketThread.start()
    super.start()
    try {
      socketThread.join(5000)
    }
    catch {
      case e: InterruptedException =>
        LOG.error(e)
    }
    if (socket == null) throw new IOException("Unable to make socket connection: " + toString) //$NON-NLS-1$
    inputStream = socket.getInputStream
    outputStream = socket.getOutputStream
  }

  override def getInputStream: InputStream = inputStream

  override def getOutputStream: OutputStream = outputStream

  override def stop(): Unit = {
    super.stop()
    if (socket != null) try
      socket.close()
    catch {
      case e: IOException =>
        LOG.error(e)
    }
  }

  override def hashCode: Int = {
    val result = super.hashCode
    result ^ Objects.hashCode(this.port)
  }
}
