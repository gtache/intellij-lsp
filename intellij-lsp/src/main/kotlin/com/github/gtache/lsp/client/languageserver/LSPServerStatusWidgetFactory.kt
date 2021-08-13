package com.github.gtache.lsp.client.languageserver

import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.prepend
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager

class LSPServerStatusWidgetFactory(private val wrapper: LanguageServerWrapper) : StatusBarWidgetFactory {
    override fun getId(): String = wrapper.project.name + "_" + wrapper.serverDefinition.ext


    override fun getDisplayName(): String = "Language server for extension ${wrapper.serverDefinition.ext}"


    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return LSPServerStatusWidget(wrapper)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
        removeWidgetID(widget as LSPServerStatusWidget)
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    companion object {
        private val widgetIDs: MutableMap<Project, MutableList<String>> = HashMap()

        /**
         * Creates a widget given a LanguageServerWrapper and adds it to the status bar
         *
         * @param wrapper The wrapper
         * @return The widget
         */
        fun createWidgetFor(wrapper: LanguageServerWrapper): LSPServerStatusWidgetFactory {
            val widgetFactory = LSPServerStatusWidgetFactory(wrapper)
            val project = wrapper.project
            val manager = project.getService(StatusBarWidgetsManager::class.java)
            manager?.updateWidget(widgetFactory)
            //statusBar.addWidget(widgetFactory, "before " + widgetIDs[project]?.head)
            widgetIDs[project].prepend(widgetFactory.id)
            return widgetFactory
        }

        fun removeWidgetID(widget: LSPServerStatusWidget): Unit {
            val project = widget.wrapper.project
            widgetIDs[project]?.remove(widget.ID())
        }
    }
}