package com.github.gtache.lsp.languageserver.definition


/**
 * List of all the configurable server definitions types
 */
enum class ConfigurableTypes(val type: String) {
    ARTIFACT(ArtifactDefinition.presentableType),
    EXE(ExeDefinition.presentableType),
    RAWCOMMAND(RawCommandDefinition.presentableType);
}