package com.github.gtache.lsp.utils.aether

interface Aether {
    fun resolveClasspath(artifact: String): String?
}
