package com.github.gtache.lsp.client.languageserver

import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.head
import com.github.gtache.lsp.prepend
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.GUIUtils
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget.IconPresentation
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.Icon

/**
 * A status bar widget for a server status
 *
 * @param wrapper The wrapper corresponding to the server
 */
class LSPServerStatusWidget(val wrapper: LanguageServerWrapper) : StatusBarWidget {

    companion object {
        private val widgetIDs: MutableMap<Project, MutableList<String>> = HashMap()

        /**
         * Creates a widget given a LanguageServerWrapper and adds it to the status bar
         *
         * @param wrapper The wrapper
         * @return The widget
         */
        fun createWidgetFor(wrapper: LanguageServerWrapper): LSPServerStatusWidget {
            val widget = LSPServerStatusWidget(wrapper)
            val project = wrapper.project
            val statusBar = WindowManager.getInstance().getStatusBar(project)
            if (!widgetIDs.containsKey(project)) {
                widgetIDs[project] = mutableListOf("Position")
            }
            statusBar.addWidget(widget, "before " + widgetIDs[project]?.head)
            widgetIDs[project].prepend(widget.ID())
            return widget
        }

        fun removeWidgetID(widget: LSPServerStatusWidget): Unit {
            val project = widget.wrapper.project
            widgetIDs[project]?.remove(widget.ID())
        }
    }

    private val timeouts: MutableMap<Timeouts, Pair<Int, Int>> = EnumMap(Timeouts::class.java)

    init {
        Timeouts.values().forEach { t -> timeouts[t] = Pair(0, 0) }
    }

    private val ext: String = wrapper.serverDefinition.ext
    private val project: Project = wrapper.project
    private val projectName: String = project.name
    private val icons: Map<ServerStatus, Icon> = GUIUtils.getIconProviderFor(wrapper.serverDefinition).statusIcons
    private var status: ServerStatus = ServerStatus.STOPPED


    fun notifyResult(timeout: Timeouts, success: Boolean): Unit {
        val oldValue = timeouts[timeout]
        timeouts[timeout] = if (success) Pair(oldValue!!.first + 1, oldValue.second) else Pair(
            oldValue!!.first,
            oldValue.second + 1
        )
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = object : IconPresentation {

        override fun getIcon(): Icon? {
            return icons[status]
        }

        override fun getClickConsumer(): Consumer<MouseEvent> = Consumer<MouseEvent> { t: MouseEvent ->
            val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
            val component = t.component
            val actions = when (wrapper.status) {
                ServerStatus.STARTED -> listOf(restart, showConnectedFiles, showTimeouts)
                ServerStatus.STARTING -> listOf(showTimeouts)
                else -> listOf(restart, showTimeouts)
            }
            val title = "Server actions for $ext - $projectName"
            val context = DataManager.getInstance().getDataContext(component)
            val group = DefaultActionGroup(actions)
            val popup = JBPopupFactory.getInstance().createActionGroupPopup(title, group, context, mnemonics, true)
            val dimension = popup.content.preferredSize
            val at = Point(0, -dimension.height)
            popup.show(RelativePoint(t.component, at))
        }

        private val restart = object :
            AnAction("&Restart the server", "Try to restart the server after it failed", AllIcons.Actions.Restart),
            DumbAware {
            override fun actionPerformed(e: AnActionEvent): Unit {
                ApplicationUtils.pool { wrapper.restart() }
            }
        }

        private val showConnectedFiles = object :
            AnAction("&Show connected files", "Show the files connected to the server", AllIcons.FileTypes.Archive),
            DumbAware {
            override fun actionPerformed(e: AnActionEvent): Unit {
                Messages.showInfoMessage(
                    "Connected files :\n" + wrapper.getConnectedFiles().joinToString("\n"),
                    "Connected files"
                )
            }
        }

        private val showTimeouts = object :
            AnAction("&Show timeouts", "Show the timeouts proportions of the server", AllIcons.General.Information),
            DumbAware {
            override fun actionPerformed(e: AnActionEvent): Unit {
                val message: StringBuilder = StringBuilder()
                message.append("<html>")
                message.append("Timeouts (failed requests) :<br>")
                timeouts.forEach { t ->
                    val timeouts = t.value.second
                    val timeout = t.key
                    message.append(timeout.name.substring(0, 1)).append(timeout.name.substring(1).lowercase())
                        .append(" -> ")
                    val total = t.value.first + timeouts
                    if (total != 0) {
                        if (timeouts > 0) message.append("<font color=\"red\">")
                        message.append(timeouts).append("/").append(total).append(" (")
                            .append(timeouts / (total * 100.0)).append("%)<br>")
                        if (timeouts > 0) message.append("</font>")
                    } else message.append("0/0 (0%)<br>")
                }
                message.append("</html>")
                Messages.showInfoMessage(message.toString(), "Timeouts")
            }
        }

        override fun getTooltipText(): String = "Language server for extension $ext, project $projectName"
    }

    override fun install(statusBar: StatusBar): Unit {}

    /**
     * Sets the status of the server
     *
     * @param status The status
     */
    fun setStatus(status: ServerStatus): Unit {
        this.status = status
        updateWidget()
    }

    private fun updateWidget(): Unit {
        val manager = WindowManager.getInstance()
        if (manager != null && !project.isDisposed) {
            manager.getStatusBar(project)?.updateWidget(ID())
        }
    }

    override fun dispose(): Unit {
        val manager = WindowManager.getInstance()
        if (manager != null && !project.isDisposed) {
            val statusBar = manager.getStatusBar(project)
            removeWidgetID(this)
            if (statusBar != null) ApplicationUtils.invokeLater { statusBar.removeWidget(ID()) }
        }
    }

    override fun ID(): String = projectName + "_" + ext
}

