package com.github.gtache.lsp.contributors.icon

import javax.swing.Icon

import com.github.gtache.lsp.ServerStatus
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.intellij.openapi.util.IconLoader
import org.eclipse.lsp4j.CompletionItemKind


object LSPDefaultIconProvider extends LSPIconProvider {
  private val STARTED = IconLoader.getIcon("/images/started.png")
  private val STARTING = IconLoader.getIcon("/images/starting.png")
  private val STOPPED = IconLoader.getIcon("/images/stopped.png")

  override def getCompletionIcon(kind: CompletionItemKind): Icon = {
    /*val icon: Icon = kind match {
  case CompletionItemKind.Class =>
  case CompletionItemKind.Color =>
  case CompletionItemKind.Constructor =>
  case CompletionItemKind.Enum =>
  case CompletionItemKind.Field =>
  case CompletionItemKind.File =>
  case CompletionItemKind.Function =>
  case CompletionItemKind.Interface =>
  case CompletionItemKind.Keyword =>
  case CompletionItemKind.Method =>
  case CompletionItemKind.Module =>
  case CompletionItemKind.Property =>
  case CompletionItemKind.Reference =>
  case CompletionItemKind.Snippet =>
  case CompletionItemKind.Text =>
  case CompletionItemKind.Unit =>
  case CompletionItemKind.Value =>
  case CompletionItemKind.Variable =>

}*/
    null
  }

  override def getStatusIcons: Map[ServerStatus, Icon] = {
    Map(ServerStatus.STOPPED -> STOPPED, ServerStatus.STARTING -> STARTING, ServerStatus.STARTED -> STARTED)
  }

  override def isSpecificFor(serverDefinition: LanguageServerDefinition): Boolean = false
}
