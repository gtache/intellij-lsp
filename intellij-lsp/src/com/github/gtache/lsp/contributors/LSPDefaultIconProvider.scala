package com.github.gtache.lsp.contributors

import javax.swing.Icon

import org.eclipse.lsp4j.CompletionItemKind

class LSPDefaultIconProvider extends LSPIconProvider {

  override def getIcon(kind: CompletionItemKind): Icon = {
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


}
