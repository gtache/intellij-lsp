package com.github.gtache.lsp.client.languageserver.serverdefinition


/**
 * List of all the configurable server definitions types
 */
enum class ConfigurableTypes(val type: String) {
    ARTIFACT(ArtifactLanguageServerDefinition.presentableType),
    EXE(ExeLanguageServerDefinition.presentableType),
    RAWCOMMAND(RawCommandServerDefinition.presentableType);
}