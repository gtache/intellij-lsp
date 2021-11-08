package com.github.gtache.lsp.settings.project

/**
 * Represents the current project lsp settings
 */
data class LSPProjectState(
    var isLoggingServersOutput: Boolean,
    var isAlwaysSendRequests: Boolean,
    var extensionsToServers: Map<String, Array<String>>,
    var forcedAssociations: Map<String, Array<String>>
) {
    constructor() : this(false, false, emptyMap(), emptyMap())

    /**
     * Returns a new State with the new [isLoggingServersOutput] value
     */
    fun withLoggingServersOutput(isLoggingServersOutput: Boolean): LSPProjectState {
        return LSPProjectState(isLoggingServersOutput, isAlwaysSendRequests, extensionsToServers, forcedAssociations)
    }

    /**
     * Returns a new State with the new [isAlwaysSendRequests] value
     */
    fun withAlwaysSendRequests(isAlwaysSendRequests: Boolean): LSPProjectState {
        return LSPProjectState(isLoggingServersOutput, isAlwaysSendRequests, extensionsToServers, forcedAssociations)
    }

    /**
     * Returns a new State with the new [extensionsToServers] values
     */
    fun withExtToServ(extensionsToServers: Map<String, Array<String>>): LSPProjectState {
        return LSPProjectState(isLoggingServersOutput, isAlwaysSendRequests, extensionsToServers, forcedAssociations)
    }

    /**
     * Returns a new State with the new [forcedAssociations] values
     */
    fun withForcedAssociations(forcedAssociations: Map<String, Array<String>>): LSPProjectState {
        return LSPProjectState(isLoggingServersOutput, isAlwaysSendRequests, extensionsToServers, forcedAssociations)
    }
}