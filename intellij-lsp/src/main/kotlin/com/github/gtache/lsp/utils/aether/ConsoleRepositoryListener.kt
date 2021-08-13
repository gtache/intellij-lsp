package com.github.gtache.lsp.utils.aether

import org.eclipse.aether.AbstractRepositoryListener
import org.eclipse.aether.RepositoryEvent
import java.io.PrintStream
import java.util.*

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
 * A simplistic repository listener that logs events to the console.
 */
class ConsoleRepositoryListener @JvmOverloads constructor(out: PrintStream? = null) : AbstractRepositoryListener() {
    private val out: PrintStream
    override fun artifactDeployed(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Deployed " + event.artifact + " to " + event.repository)
    }

    override fun artifactDeploying(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Deploying " + event.artifact + " to " + event.repository)
    }

    override fun artifactDescriptorInvalid(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println(
            "Invalid artifact descriptor for " + event.artifact + ": "
                    + event.exception.message
        )
    }

    override fun artifactDescriptorMissing(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Missing artifact descriptor for " + event.artifact)
    }

    override fun artifactInstalled(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Installed " + event.artifact + " to " + event.file)
    }

    override fun artifactInstalling(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Installing " + event.artifact + " to " + event.file)
    }

    override fun artifactResolved(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Resolved artifact " + event.artifact + " from " + event.repository)
    }

    override fun artifactDownloading(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Downloading artifact " + event.artifact + " from " + event.repository)
    }

    override fun artifactDownloaded(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Downloaded artifact " + event.artifact + " from " + event.repository)
    }

    override fun artifactResolving(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Resolving artifact " + event.artifact)
    }

    override fun metadataDeployed(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Deployed " + event.metadata + " to " + event.repository)
    }

    override fun metadataDeploying(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Deploying " + event.metadata + " to " + event.repository)
    }

    override fun metadataInstalled(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Installed " + event.metadata + " to " + event.file)
    }

    override fun metadataInstalling(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Installing " + event.metadata + " to " + event.file)
    }

    override fun metadataInvalid(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Invalid metadata " + event.metadata)
    }

    override fun metadataResolved(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Resolved metadata " + event.metadata + " from " + event.repository)
    }

    override fun metadataResolving(event: RepositoryEvent) {
        Objects.requireNonNull(event, "event cannot be null")
        out.println("Resolving metadata " + event.metadata + " from " + event.repository)
    }

    init {
        this.out = out ?: System.out
    }
}