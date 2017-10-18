package com.github.gtache.contributors

import com.github.gtache.PluginMain
import com.intellij.navigation.{ChooseByNameContributor, NavigationItem}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
  * The Go-to contributor for LSP
  */
class LSPGotoContributor extends ChooseByNameContributor {
  private val LOG: Logger = Logger.getInstance(classOf[LSPGotoContributor])

  override def getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array[NavigationItem] = {
    val res = PluginMain.workspaceSymbols(name, pattern, project, includeNonProjectItems)
    LOG.info("Getting items by name : " + res.length)
    res
  }

  override def getNames(project: Project, includeNonProjectItems: Boolean): Array[String] = {
    val res = PluginMain.allWorkspaceSymbols(project)
    LOG.info("Getting names : " + res.mkString(";"))
    res
  }
}
