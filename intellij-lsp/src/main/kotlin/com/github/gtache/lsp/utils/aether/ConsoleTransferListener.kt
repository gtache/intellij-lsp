package com.github.gtache.lsp.utils.aether

import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transfer.MetadataNotFoundException
import org.eclipse.aether.transfer.TransferEvent
import org.eclipse.aether.transfer.TransferResource
import java.io.PrintStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/ /**
 * A simplistic transfer listener that logs uploads/downloads to the console.
 */
class ConsoleTransferListener @JvmOverloads constructor(out: PrintStream? = null) : AbstractTransferListener() {
    private val out: PrintStream
    private val downloads: MutableMap<TransferResource, Long> = ConcurrentHashMap()
    private var lastLength = 0
    override fun transferInitiated(event: TransferEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        val message = if (event.requestType == TransferEvent.RequestType.PUT) "Uploading" else "Downloading"
        out.println(message + ": " + event.resource.repositoryUrl + event.resource.resourceName)
    }

    override fun transferProgressed(event: TransferEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        val resource = event.resource
        downloads[resource] = event.transferredBytes
        val buffer = StringBuilder(64)
        for (entry in downloads.entries) {
            val total: Long = entry.key.contentLength
            val complete: Long = entry.value
            buffer.append(getStatus(complete, total)).append("  ")
        }
        val pad = lastLength - buffer.length
        lastLength = buffer.length
        pad(buffer, pad)
        buffer.append('\r')
        out.print(buffer)
    }

    private fun getStatus(complete: Long, total: Long): String {
        if (total >= 1024) {
            return toKB(complete).toString() + "/" + toKB(total) + " KB "
        } else if (total >= 0) {
            return "$complete/$total B "
        } else return if (complete >= 1024) {
            toKB(complete).toString() + " KB "
        } else {
            "$complete B "
        }
    }

    private fun pad(buffer: StringBuilder, spaces: Int) {
        var spaces = spaces
        val block = "                                        "
        while (spaces > 0) {
            val n = Math.min(spaces, block.length)
            buffer.append(block, 0, n)
            spaces -= n
        }
    }

    override fun transferSucceeded(event: TransferEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        transferCompleted(event)
        val resource = event.resource
        val contentLength = event.transferredBytes
        if (contentLength >= 0) {
            val type = if (event.requestType == TransferEvent.RequestType.PUT) "Uploaded" else "Downloaded"
            val len = if (contentLength >= 1024) toKB(contentLength).toString() + " KB" else "$contentLength B"
            var throughput = ""
            val duration = System.currentTimeMillis() - resource.transferStartTime
            if (duration > 0) {
                val bytes = contentLength - resource.resumeOffset
                val format = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
                val kbPerSec = bytes / 1024.0 / (duration / 1000.0)
                throughput = " at " + format.format(kbPerSec) + " KB/sec"
            }
            out.println(
                type + ": " + resource.repositoryUrl + resource.resourceName + " (" + len
                        + throughput + ")"
            )
        }
    }

    override fun transferFailed(event: TransferEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        transferCompleted(event)
        if (event.exception !is MetadataNotFoundException) {
            event.exception.printStackTrace(out)
        }
    }

    private fun transferCompleted(event: TransferEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        downloads.remove(event.resource)
        val buffer = StringBuilder(64)
        pad(buffer, lastLength)
        buffer.append('\r')
        out.print(buffer)
    }

    override fun transferCorrupted(event: TransferEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        event.exception.printStackTrace(out)
    }

    protected fun toKB(bytes: Long): Long {
        return (bytes + 1023) / 1024
    }

    init {
        this.out = out ?: System.out
    }
}