package com.github.gtache.lsp.client.languageserver

import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager

class LSPServerStatusWidgetFactory : StatusBarWidgetFactory {
    private val wrappers: MutableMap<Project, MutableList<LanguageServerWrapper>> = HashMap()

    override fun getId(): String = Companion.id

    override fun getDisplayName(): String = "Language servers"

    override fun isAvailable(project: Project): Boolean = false//wrappers.isNotEmpty() && wrappers[project]?.isEmpty() != true

    override fun createWidget(project: Project): StatusBarWidget {
        return LSPServerStatusWidget(wrappers[project] ?: emptyList(), project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    /**
     * Creates a widget given a LanguageServerWrapper and adds it to the status bar
     *
     * @param wrapper The wrapper
     * @return The widget
     */
    fun addWrapper(wrapper: LanguageServerWrapper): Unit {
        val project = wrapper.project
        if (!wrappers.contains(project)) {
            wrappers[project] = mutableListOf(wrapper)
        } else {
            val projectWrappers = wrappers[project]!!
            if (!projectWrappers.contains(wrapper)) {
                projectWrappers += wrapper
                updateWidget(wrapper)
            } else {
                logger.warn("Trying to add already existing wrapper $wrapper")
            }
        }
    }

    fun removeWrapper(wrapper: LanguageServerWrapper): Unit {
        val project = wrapper.project
        if (!wrappers.contains(project)) {
            logger.warn("Trying to remove non-existing wrapper $wrapper")
        } else {
            val projectWrappers = wrappers[project]!!
            if (projectWrappers.contains(wrapper)) {
                projectWrappers -= wrapper
                if (projectWrappers.isEmpty()) {
                    wrappers.remove(wrapper.project)
                }
                updateWidget(wrapper)
            } else {
                logger.warn("Trying to remove non-existing wrapper $wrapper")
            }
        }
    }

    private fun updateWidget(wrapper: LanguageServerWrapper) {
        wrapper.project.getService(StatusBarWidgetsManager::class.java).updateWidget(this)
    }

    companion object {
        private val logger: Logger = Logger.getInstance(LSPServerStatusWidgetFactory::class.java)
        const val id = "LSPStatusWidgetFactory"
    }
}