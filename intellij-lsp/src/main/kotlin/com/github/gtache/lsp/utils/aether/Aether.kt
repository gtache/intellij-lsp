package com.github.gtache.lsp.utils.aether

/**
 * Represents an Aether object used to retrieve artifacts
 */
interface Aether {
    /**
     * Resolves the classpath for the given [artifact] and possibly downloads it
     */
    fun resolveClasspath(artifact: String): String?
}
