package com.github.gtache.lsp.client.languageserver

import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.GUIUtils
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Condition
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.util.Consumer
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.Icon

/**
 * A status bar widget for a server status
 *
 * @param wrapper The wrapper corresponding to the server
 */
class LSPServerStatusWidget(private var wrappers: List<LanguageServerWrapper>, private val project: Project) : StatusBarWidget {

    init {
        widgets[project] = this
    }

    companion object {
        val widgets: MutableMap<Project, LSPServerStatusWidget> = HashMap()
    }

    private var statusBar: StatusBar? = null
    private val timeouts: MutableMap<Timeouts, Pair<Int, Int>> = EnumMap(Timeouts::class.java)

    init {
        Timeouts.values().forEach { t -> timeouts[t] = Pair(0, 0) }
    }

    fun notifyResult(timeout: Timeouts, success: Boolean): Unit {
        val oldValue = timeouts[timeout]
        timeouts[timeout] = if (success) Pair(oldValue!!.first + 1, oldValue.second) else Pair(
            oldValue!!.first,
            oldValue.second + 1
        )
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = object : StatusBarWidget.MultipleTextValuesPresentation {

        override fun getIcon(): Icon? {
            return AllIcons.General.Settings
        }

        override fun getClickConsumer(): Consumer<MouseEvent> = Consumer<MouseEvent> { t: MouseEvent ->
//            val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
//            val component = t.component
//            val actions = when (wrapper.status) {
//                ServerStatus.STARTED -> listOf(restart, showConnectedFiles, showTimeouts)
//                ServerStatus.STARTING -> listOf(showTimeouts)
//                else -> listOf(restart, showTimeouts)
//            }
//            val title = "Server actions for $ext - $projectName"
//            val context = DataManager.getInstance().getDataContext(component)
//            val group = DefaultActionGroup(actions)
//            val popup = JBPopupFactory.getInstance().createActionGroupPopup(title, group, context, mnemonics, true)
//            val dimension = popup.content.preferredSize
//            val at = Point(0, -dimension.height)
//            popup.show(RelativePoint(t.component, at))
        }

        override fun getPopupStep(): ListPopup {
            val context = DataManager.getInstance().getDataContext(statusBar!!.component)
            return PopupFactoryImpl.ActionGroupPopup(
                "List of servers for $project", createActionGroup(), context, false, false,
                false, false, null, -1, Condition { true }, null
            )
        }

        fun createActionGroup(): ActionGroup {
            val actions = wrappers.map { w ->
                object : AnAction(
                    w.serverDefinition.ext,
                    "Language server for ${w.serverDefinition.ext}",
                    GUIUtils.getIconProviderFor(w.serverDefinition).statusIcons[w.status]
                ) {
                    override fun actionPerformed(e: AnActionEvent) {
                        TODO("Not yet implemented")
                    }
                }
            }
            return object : DefaultActionGroup("List of language servers", actions), AlwaysVisibleActionGroup {}
        }

        override fun getSelectedValue(): String {
            TODO("Not yet implemented")
        }

        inner class RestartAction(private val wrapper: LanguageServerWrapper) :
            AnAction("&Restart the server", "Try to restart the server after it failed", AllIcons.Actions.Restart), DumbAware {
            override fun actionPerformed(e: AnActionEvent): Unit {
                ApplicationUtils.pool { wrapper.restart() }
            }
        }

        inner class ShowConnectedFilesAction(private val wrapper: LanguageServerWrapper) :
            AnAction("&Show connected files", "Show the files connected to the server", AllIcons.FileTypes.Archive), DumbAware {
            override fun actionPerformed(e: AnActionEvent): Unit {
                Messages.showInfoMessage(
                    "Connected files :\n" + wrapper.getConnectedFiles().joinToString("\n"),
                    "Connected files"
                )
            }
        }

        inner class ShowTimeoutsAction(private val wrapper: LanguageServerWrapper) :
            AnAction("&Show timeouts", "Show the timeouts proportions of the server", AllIcons.General.Information), DumbAware {
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

        override fun getTooltipText(): String = "Language Server Protocol"
    }

    override fun install(statusBar: StatusBar): Unit {
        this.statusBar = statusBar
    }

    /**
     * Sets the status of the server
     *
     * @param status The status
     */
    fun statusUpdated(wrapper: LanguageServerWrapper): Unit {
        if (wrappers.contains(wrapper)) {
            updateWidget()
        }
    }

    fun setWrappers(wrappers: List<LanguageServerWrapper>): Unit {
        this.wrappers = wrappers.toList()
        updateWidget()
    }

    private fun updateWidget(): Unit {
        statusBar?.updateWidget(ID())
    }

    override fun dispose(): Unit {
    }

    override fun ID(): String = "LSPStatusWidget"
}

