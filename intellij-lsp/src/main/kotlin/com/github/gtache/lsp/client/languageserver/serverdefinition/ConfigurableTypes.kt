package com.github.gtache.lsp.client.languageserver.serverdefinition


enum class ConfigurableTypes(val typ: String) {
    ARTIFACT(ArtifactLanguageServerDefinition.presentableTyp),
    EXE(ExeLanguageServerDefinition.presentableTyp),
    RAWCOMMAND(RawCommandServerDefinition.presentableTyp);
}