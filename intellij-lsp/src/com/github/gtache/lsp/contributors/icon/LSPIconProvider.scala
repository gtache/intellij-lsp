package com.github.gtache.lsp.contributors.icon

import javax.swing.Icon

import com.github.gtache.lsp.ServerStatus
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.intellij.openapi.extensions.ExtensionPointName
import org.eclipse.lsp4j.CompletionItemKind

object LSPIconProvider {
  val EP_NAME: ExtensionPointName[LSPIconProvider] = ExtensionPointName.create("com.github.gtache.lsp.contributors.icon.LSPIconProvider")
}

trait LSPIconProvider {

  def getCompletionIcon(kind: CompletionItemKind): Icon

  def getStatusIcons : Map[ServerStatus, Icon]

  def isSpecificFor(serverDefinition: LanguageServerDefinition) : Boolean
}
