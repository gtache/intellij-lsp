package com.github.gtache.contributors

import javax.swing.Icon

import com.intellij.icons.AllIcons
import com.intellij.navigation.{ItemPresentation, NavigationItem}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

case class LSPNavigationItem(name: String, container: String, project: Project, file: VirtualFile, line: Int, col: Int) extends OpenFileDescriptor(project, file, line, col) with NavigationItem {

  private val LOG: Logger = Logger.getInstance(classOf[LSPNavigationItem])

  override def getName: String = name

  override def getPresentation: ItemPresentation = new ItemPresentation {

    override def getPresentableText: String = name

    override def getLocationString: String = (if (container != null) container else "") + name

    override def getIcon(unused: Boolean): Icon = if (unused) null else AllIcons.Icon_small
  }
}
