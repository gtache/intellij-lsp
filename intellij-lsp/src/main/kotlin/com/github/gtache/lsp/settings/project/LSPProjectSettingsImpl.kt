package com.github.gtache.lsp.settings.project

import com.github.gtache.lsp.services.project.LSPProjectService
import com.github.gtache.lsp.utils.ApplicationUtils
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.util.*

/**
 * Class representing the state of the LSP settings
 */
@State(name = "LSPProjectState", storages = [Storage(value = "LSPProjectState.xml")])
class LSPProjectSettingsImpl(private val project: Project) : LSPProjectSettings {

    override var projectState: LSPProjectState = LSPProjectState()

    override fun hashCode(): Int {
        return Objects.hash(project, projectState)
    }

    override fun equals(other: Any?): Boolean {
        return (other is LSPProjectSettingsImpl) && project == other.project && projectState == other.projectState
    }

    override fun getState(): LSPProjectState {
        return projectState
    }

    override fun loadState(state: LSPProjectState) {
        projectState = state
        logger.info("LSP Project State loaded")
        ApplicationUtils.pool {
            project.service<LSPProjectService>().notifyStateLoaded()
        }
    }

    companion object {
        private val logger = Logger.getInstance(LSPProjectSettingsImpl::class.java)
    }
}