package com.github.gtache.lsp.settings.server

import com.intellij.openapi.extensions.ExtensionPointName

trait LSPConfiguration {
  def getSettings: java.util.Map[String, java.util.Map[String, AnyRef]]
}

object LSPConfiguration {
  val EP_NAME: ExtensionPointName[LSPConfiguration] = ExtensionPointName.create("com.github.gtache.lsp.contributors.icon.lspConfiguration")

}