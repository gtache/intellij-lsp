package com.github.gtache.lsp.settings

import com.github.gtache.lsp.LSPProjectService
import com.github.gtache.lsp.utils.ApplicationUtils
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.xmlb.XmlSerializerUtil
import java.util.*

@State(name = "LSPProjectState")
/**
 * Class representing the state of the LSP settings
 */
class LSPProjectStateImpl(private val project: Project) : LSPProjectState, PersistentStateComponent<LSPProjectStateImpl> {

    //Must be public to be saved
    override var isLoggingServersOutput = false
    override var isAlwaysSendRequests = false
    override var extToServ: Map<String, Array<String>> = emptyMap()
        get() = field.mapValues { it.value.copyOf() }.toMap()
        set(value) {
            field = value.mapValues { it.value.copyOf() }.toMap()
        }
    override var forcedAssociations: Map<String, Array<String>> = emptyMap()
        get() = field.toMap()
        set(value) {
            field = value.toMap()
        }

    override fun hashCode(): Int {
        return Objects.hash(isLoggingServersOutput, isAlwaysSendRequests, extToServ, forcedAssociations)
    }

    override fun equals(other: Any?): Boolean {
        return (other is LSPProjectStateImpl) && isLoggingServersOutput == other.isLoggingServersOutput && isAlwaysSendRequests == other.isAlwaysSendRequests && extToServ == other.extToServ && forcedAssociations == other.forcedAssociations
    }

    override fun getState(): LSPProjectStateImpl {
        return this
    }

    override fun loadState(lspState: LSPProjectStateImpl) {
        try {
            XmlSerializerUtil.copyBean(lspState, this)
            logger.info("LSP Project State loaded")
            project.service<LSPProjectService>().notifyStateLoaded()
        } catch (e: Exception) {
            logger.warn("Couldn't load LSP Project State : $e")
            ApplicationUtils.invokeLater {
                Messages.showErrorDialog(
                    "Couldn't load LSP settings, you will need to reconfigure them.",
                    "LSP plugin"
                )
            }
        }
    }

    companion object {
        private val logger = Logger.getInstance(LSPProjectStateImpl::class.java)
    }
}