package com.github.gtache.lsp.settings.project

import com.github.gtache.lsp.utils.ApplicationUtils
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.util.*

/**
 * Class managing the persistent state of the LSP project settings
 */
@State(name = "LSPProjectState", storages = [Storage(value = "LSPProjectState.xml")])
class LSPPersistentProjectSettingsImpl(private val project: Project) : LSPPersistentProjectSettings {

    private val listeners: MutableSet<LSPPersistentProjectSettings.Listener> = HashSet()
    override var projectState: LSPProjectState = LSPProjectState()
        set(value) {
            val oldValue = field
            field = value
            notifyListeners(oldValue, value)
        }


    override fun addListener(listener: LSPPersistentProjectSettings.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: LSPPersistentProjectSettings.Listener) {
        listeners.remove(listener)
    }

    override fun getListeners(): Set<LSPPersistentProjectSettings.Listener> {
        return listeners.toSet()
    }

    override fun hashCode(): Int {
        return Objects.hash(project, projectState)
    }

    override fun equals(other: Any?): Boolean {
        return (other is LSPPersistentProjectSettingsImpl) && project == other.project && projectState == other.projectState
    }

    override fun getState(): LSPProjectState {
        return projectState
    }

    override fun loadState(state: LSPProjectState) {
        val oldState = projectState
        projectState = state
        logger.info("LSP Project State loaded")

        notifyListeners(oldState, projectState)
    }

    private fun notifyListeners(oldState: LSPProjectState, newState: LSPProjectState) {
        ApplicationUtils.pool {
            listeners.forEach { l -> l.stateChanged(oldState, newState) }
        }
    }

    companion object {
        private val logger = Logger.getInstance(LSPPersistentProjectSettingsImpl::class.java)
    }
}