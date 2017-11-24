package com.github.gtache.lsp

import java.awt.event.MouseEvent
import javax.swing.Icon

import com.github.gtache.lsp.client.languageserver.ServerStatus
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.utils.GUIUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget.IconPresentation
import com.intellij.openapi.wm.{StatusBar, StatusBarWidget, WindowManager}
import com.intellij.util.Consumer

object LSPServerStatusWidget {

  private var lastWidgetID: String = _

  /**
    * Creates a widget given a LanguageServerWrapper and adds it to the status bar
    *
    * @param wrapper The wrapper
    * @return The widget
    */
  def createWidgetFor(wrapper: LanguageServerWrapper): LSPServerStatusWidget = {
    val widget = new LSPServerStatusWidget(wrapper)
    val statusBar = WindowManager.getInstance().getStatusBar(wrapper.getProject)
    if (lastWidgetID != null) statusBar.addWidget(widget, "before " + lastWidgetID) else statusBar.addWidget(widget, "before Position")
    lastWidgetID = widget.ID()
    widget
  }

}

/**
  * A status bar widget for a server status
  *
  * @param wrapper The wrapper corresponding to the server
  */
class LSPServerStatusWidget(wrapper: LanguageServerWrapper) extends StatusBarWidget {

  private val ext: String = wrapper.getServerDefinition.ext
  private val project: Project = wrapper.getProject
  private val projectName: String = project.getName
  private val icons: Map[ServerStatus, Icon] = GUIUtils.getIconProviderFor(wrapper.getServerDefinition).getStatusIcons
  private var status: ServerStatus = ServerStatus.STOPPED

  override def getPresentation(`type`: StatusBarWidget.PlatformType): StatusBarWidget.IconPresentation = new IconPresentation {

    override def getIcon: Icon = {
      icons.get(status).orNull
    }

    override def getClickConsumer: Consumer[MouseEvent] = (t: MouseEvent) => {}

    override def getTooltipText: String = "Language server for extension " + ext + ", project " + projectName
  }

  override def install(statusBar: StatusBar): Unit = {}

  /**
    * Sets the status of the server
    *
    * @param status The new status
    */
  def setStatus(status: ServerStatus): Unit = {
    this.status = status
    updateWidget()
  }

  private def updateWidget(): Unit = {
    val manager = WindowManager.getInstance()
    if (manager != null && project != null) {
      val statusBar = manager.getStatusBar(project)
      if (statusBar != null) {
        statusBar.updateWidget(ID())
      }
    }
  }

  override def ID(): String = projectName + "_" + ext

  override def dispose(): Unit = {}
}


