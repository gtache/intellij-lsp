package com.github.gtache.lsp.settings.project

data class LSPProjectState(
    var isLoggingServersOutput: Boolean,
    var isAlwaysSendRequests: Boolean,
    var extToServ: Map<String, Array<String>>,
    var forcedAssociations: Map<String, Array<String>>
) {
    constructor() : this(false, false, emptyMap(), emptyMap())

    fun withLoggingServersOutput(isLoggingServersOutput: Boolean): LSPProjectState {
        return LSPProjectState(isLoggingServersOutput, isAlwaysSendRequests, extToServ, forcedAssociations)
    }

    fun withAlwaysSendRequests(isAlwaysSendRequests: Boolean): LSPProjectState {
        return LSPProjectState(isLoggingServersOutput, isAlwaysSendRequests, extToServ, forcedAssociations)
    }

    fun withExtToServ(extToServ: Map<String, Array<String>>): LSPProjectState {
        return LSPProjectState(isLoggingServersOutput, isAlwaysSendRequests, extToServ, forcedAssociations)
    }

    fun withForcedAssociations(forcedAssociations: Map<String, Array<String>>): LSPProjectState {
        return LSPProjectState(isLoggingServersOutput, isAlwaysSendRequests, extToServ, forcedAssociations)
    }
}