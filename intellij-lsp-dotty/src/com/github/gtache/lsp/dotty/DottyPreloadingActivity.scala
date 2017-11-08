package com.github.gtache.lsp.dotty

import com.github.gtache.lsp.client.languageserver.serverdefinition.{ArtifactLanguageServerDefinition, LanguageServerDefinition}
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.progress.ProgressIndicator

class DottyPreloadingActivity extends PreloadingActivity {
  override def preload(progressIndicator: ProgressIndicator): Unit = {
    LanguageServerDefinition.register(new ArtifactLanguageServerDefinition("scala", "ch.epfl.lamp:dotty-language-server_0.3:0.3.0-RC2", "dotty.tools.languageserver.Main", Array[String]("-stdio")))
  }
}