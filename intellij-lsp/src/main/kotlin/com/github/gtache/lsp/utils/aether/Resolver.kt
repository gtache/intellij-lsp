package com.github.gtache.lsp.utils.aether

import com.google.inject.Guice
import com.intellij.openapi.diagnostic.Logger
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.installation.InstallRequest
import org.eclipse.aether.installation.InstallationException
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResolutionException
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator

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
*/
class Resolver(private val remoteRepository: String, localRepository: String) {
    private val repositorySystem: RepositorySystem = newRepositorySystem()
    private val localRepository: LocalRepository = LocalRepository(localRepository)

    private fun newSession(): RepositorySystemSession {
        val session = MavenRepositorySystemUtils.newSession()
        session.transferListener = ConsoleTransferListener()
        session.repositoryListener = ConsoleRepositoryListener()
        session.localRepositoryManager = repositorySystem.newLocalRepositoryManager(session, localRepository)

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );
        return session
    }

    @Throws(DependencyResolutionException::class)
    fun resolve(groupId: String, artifactId: String, version: String): ResolverResult {
        val session = newSession()
        val dependency = Dependency(DefaultArtifact(groupId, artifactId, "", "jar", version), "runtime")
        val central = RemoteRepository.Builder(
            "central", "default",
            remoteRepository
        ).build()
        val collectRequest = CollectRequest()
        collectRequest.root = dependency
        collectRequest.addRepository(central)
        val dependencyRequest = DependencyRequest()
        dependencyRequest.collectRequest = collectRequest
        val rootNode = repositorySystem.resolveDependencies(session, dependencyRequest).root
        val nlg = PreorderNodeListGenerator()
        rootNode.accept(nlg)
        return ResolverResult(rootNode, nlg.files, nlg.classPath)
    }

    @Throws(InstallationException::class)
    fun install(artifact: Artifact, pom: Artifact) {
        val session = newSession()
        val installRequest = InstallRequest()
        installRequest.addArtifact(artifact).addArtifact(pom)
        repositorySystem.install(session, installRequest)
    }

    companion object {
        private val logger = Logger.getInstance(Resolver::class.java)
        private val injector = Guice.createInjector(LSPAetherModule())
        private fun newRepositorySystem(): RepositorySystem {
            return injector.getInstance(RepositorySystem::class.java)
        }
    }
}