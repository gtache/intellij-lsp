package com.github.gtache.contributors

import com.intellij.navigation.{ChooseByNameContributor, NavigationItem}
import com.intellij.openapi.project.Project

/**
  * The Go-to contributor for LSP
  */
class LSPGotoContributor extends ChooseByNameContributor {

  override def getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array[NavigationItem] = {
    Array()
  }

  override def getNames(project: Project, includeNonProjectItems: Boolean): Array[String] = {
    Array()
  }
}
