package com.github.gtache.contributors

import com.github.gtache.PluginMain
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

trait LSPGotoContributor extends ChooseByNameContributor {
  protected val LOG: Logger = Logger.getInstance(this.getClass)

  override def getNames(project: Project, includeNonProjectItems: Boolean): Array[String] = {
    val res = PluginMain.workspaceSymbols("", "", project, includeNonProjectItems).map(f => f.getName)
    res
  }
}
