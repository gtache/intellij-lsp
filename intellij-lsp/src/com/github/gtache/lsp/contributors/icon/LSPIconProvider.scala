package com.github.gtache.lsp.contributors.icon

import javax.swing.Icon

import com.github.gtache.lsp.ServerStatus
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.intellij.openapi.extensions.ExtensionPointName
import org.eclipse.lsp4j.{CompletionItemKind, SymbolInformation, SymbolKind}

object LSPIconProvider {
  val EP_NAME: ExtensionPointName[LSPIconProvider] = ExtensionPointName.create("com.github.gtache.lsp.contributors.icon.LSPIconProvider")

  def getDefaultCompletionIcon(kind: CompletionItemKind) : Icon = LSPDefaultIconProvider.getCompletionIcon(kind)

  def getDefaultStatusIcons: Map[ServerStatus, Icon] = LSPDefaultIconProvider.getStatusIcons

  def getDefaultSymbolIcon(kind: SymbolKind) : Icon = LSPDefaultIconProvider.getSymbolIcon(kind)
}

trait LSPIconProvider {

  def getCompletionIcon(kind: CompletionItemKind): Icon = LSPDefaultIconProvider.getCompletionIcon(kind)

  def getStatusIcons : Map[ServerStatus, Icon] = LSPDefaultIconProvider.getStatusIcons

  def getSymbolIcon(kind: SymbolKind) : Icon = LSPDefaultIconProvider.getSymbolIcon(kind)

  def isSpecificFor(serverDefinition: LanguageServerDefinition) : Boolean
}
