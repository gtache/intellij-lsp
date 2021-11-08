package com.github.gtache.lsp.utils.aether

import com.github.gtache.lsp.head
import com.github.gtache.lsp.runCommand
import com.github.gtache.lsp.settings.application.LSPApplicationSettings
import com.github.gtache.lsp.utils.ApplicationUtils.invokeLater
import com.github.gtache.lsp.utils.Utils
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyResolutionException
import java.io.File

/**
 * Aether service
 */
class AetherImpl : Aether {

    override fun resolveClasspath(artifact: String): String? {
        val dep = parseDepString(artifact)
        val resolvers = listOf(Resolver(CENTRAL.url, LOCAL_REPOSITORY)) + getAdditionalRepositories().map { rp -> Resolver(rp.url, LOCAL_REPOSITORY) }
        var result: ResolverResult? = null
        for (resolver in resolvers) {
            try {
                result = resolver.resolve(dep.first, dep.second, dep.third)
            } catch (e: DependencyResolutionException) {

            }
        }
        return result?.resolvedClassPath
    }

    private fun parseDepString(str: String): Triple<String, String, String> {
        val res = str.split(":")
        if (res.size != 3) {
            throw AetherException("Unknown dependency format : $str")
        } else {
            return Triple(res[0], res[1], res[2])
        }
    }


    private fun getAdditionalRepositories(): Iterable<RemoteRepository> {
        val repos = service<LSPApplicationSettings>().appState.additionalRepositories
        return if (!checkRepositories(repos, showErrorMessage = false)) {
            invokeLater { Messages.showErrorDialog("Malformed artifact repositories, please check LSP settings", "Artifact retrieval error") }
            emptyList()
        } else {
            val additionalRepos = repos.map { r -> r.split(SEPARATOR) }.partition { arr -> arr[0].equals(Repositories.IVY.name, true) }
            additionalRepos.second.map { arr -> RemoteRepository.Builder(arr[1], "default", arr[1]).build() }
        }
    }

    companion object {
        private const val SEPARATOR = "::"

        private val CENTRAL: RemoteRepository = RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2").build()

        private const val LOCAL_REPO_NAME = "localRepository"
        private val LOCAL_REPOSITORY: String

        init {
            val effectiveSettings = "mvn help:effective-settings".runCommand(File("."))
            LOCAL_REPOSITORY = if (effectiveSettings != null && effectiveSettings.contains(LOCAL_REPO_NAME)) {
                val localRepoLine = effectiveSettings.split("\n").filter { it.contains(LOCAL_REPO_NAME) }.head
                val split = localRepoLine.split("[<>]")
                split.filter { !it.contains(LOCAL_REPO_NAME) && it.contains(File.separator) }.head
            } else {
                val home = System.getProperty("user.home").trimEnd('/')
                home + File.separator + ".m2" + File.separator + "repository"
            }
        }

        /**
         * Checks the [repositories] string syntax and possibly shows an error message
         */
        @JvmStatic
        fun checkRepositories(repositories: String, showErrorMessage: Boolean = false): Boolean {
            return if (repositories.isNotEmpty()) {
                val repos = repositories.split("\n")
                checkRepositories(repos, showErrorMessage)
            } else true
        }

        /**
         * Checks the [repos] syntax and possibly shows an error message
         */
        fun checkRepositories(repos: Iterable<String>, showErrorMessage: Boolean): Boolean {
            val errMsg = StringBuilder(0)
            return if (showErrorMessage) {
                repos.forEach { s ->
                    val arr = s.split(SEPARATOR)
                    if (arr.isEmpty() || arr.size != 2 || !Repositories.values().map { v -> v.name.lowercase() }.contains(arr[0].lowercase()))
                        errMsg.append(arr.joinToString("-")).append(Utils.LINE_SEPARATOR)
                }
                if (errMsg.isNotEmpty()) {
                    invokeLater {
                        Messages.showErrorDialog(
                            errMsg.insert(
                                0,
                                "The repositories syntax is incorrect : they should look like maven::http://... separated by a new line. Errors : \n"
                            ).toString(), "Repositories error"
                        )
                    }
                    false
                } else true
            } else {
                val res = repos.all { s ->
                    val arr = s.split(SEPARATOR)
                    arr.size == 2 && Repositories.values().map { v -> v.name.lowercase() }.contains(arr[0].lowercase())
                }
                res
            }
        }
    }
}