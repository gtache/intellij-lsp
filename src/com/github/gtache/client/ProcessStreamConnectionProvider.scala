/** *****************************************************************************
  * Copyright (c) 2016 Rogue Wave Software Inc. and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * Michał Niewrzał (Rogue Wave Software Inc.) - initial implementation
  * ******************************************************************************/
package com.github.gtache.client

import java.io.{File, IOException, InputStream, OutputStream}
import java.util.Objects

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.annotations.Nullable

class ProcessStreamConnectionProvider(private var commands: Seq[String], private var workingDir: String) extends StreamConnectionProvider {
  @Nullable private var process: Process = _
  private val LOG: Logger = Logger.getInstance(classOf[ProcessOverSocketStreamConnectionProvider])

  @throws[IOException]
  override def start(): Unit = {
    if (this.workingDir == null || this.commands == null || this.commands.isEmpty || this.commands.contains(null)) throw new IOException("Unable to start language server: " + this.toString) //$NON-NLS-1$
    val builder = createProcessBuilder
    LOG.info("Starting server process with commands " + commands + " and workingDir " + workingDir)
    this.process = builder.start

    if (!process.isAlive) throw new IOException("Unable to start language server: " + this.toString) else LOG.info("Server process started " + process)
  }

  protected def createProcessBuilder: ProcessBuilder = {
    import scala.collection.JavaConverters._
    val builder = new ProcessBuilder(getCommands.asJava)
    builder.directory(new File(getWorkingDirectory))
    builder.redirectError(ProcessBuilder.Redirect.INHERIT)
    builder
  }

  @Nullable override def getInputStream: InputStream = {
    if (process == null) null
    else process.getInputStream
  }

  @Nullable override def getOutputStream: OutputStream = {
    if (process == null) null
    else process.getOutputStream
  }

  override def stop(): Unit = {
    if (process != null) process.destroy()
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case other: ProcessStreamConnectionProvider =>
        getCommands.size == other.getCommands.size && this.getCommands.toSet == other.getCommands.toSet && this.getWorkingDirectory == other.getWorkingDirectory
      case _ => false
    }

  }

  override def hashCode: Int = {
    Objects.hashCode(this.getCommands) ^ Objects.hashCode(this.getWorkingDirectory)
  }

  protected def getCommands: Seq[String] = commands

  def setCommands(commands: Seq[String]): Unit = {
    this.commands = commands
  }

  protected def getWorkingDirectory: String = workingDir

  def setWorkingDirectory(workingDir: String): Unit = {
    this.workingDir = workingDir
  }
}
