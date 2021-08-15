package com.github.gtache.lsp.settings

interface LSPProjectState {
    var isLoggingServersOutput: Boolean
    var isAlwaysSendRequests: Boolean
    var extToServ: Map<String, Array<String>>
    var forcedAssociations: Map<String, Array<String>>
}