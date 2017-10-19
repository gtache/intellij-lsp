package com.github.gtache.contributors

import com.github.gtache.PluginMain
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.SymbolKind

class LSPGotoClassContributor extends LSPGotoContributor {
  override def getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array[NavigationItem] = {
    val res = PluginMain.workspaceSymbols(if (name.endsWith("$")) name.dropRight(1) else name, pattern, project, includeNonProjectItems, Set(SymbolKind.Class))
    res
  }

}
