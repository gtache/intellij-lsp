package com.github.gtache.lsp.settings

import com.github.gtache.lsp.requests.Timeout
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.utils.ApplicationUtils
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.util.xmlb.XmlSerializerUtil
import java.util.*

@State(name = "LSPApplicationState", storages = [Storage(value = "LSPApplicationState.xml")])
class LSPApplicationStateImpl : LSPApplicationState, PersistentStateComponent<LSPApplicationStateImpl> {
    override var timeouts: Map<Timeouts, Long> = emptyMap()
        get() = field.toMap()
        set(value) {
            field = value.toMap()
        }
    override var additionalRepositories: List<String> = emptyList()
        get() = field.toList()
        set(value) {
            field = value.toList()
        }

    override fun hashCode(): Int {
        return Objects.hash(timeouts, additionalRepositories)
    }

    override fun equals(other: Any?): Boolean {
        return (other is LSPApplicationStateImpl) && timeouts == other.timeouts && additionalRepositories == other.additionalRepositories
    }

    override fun getState(): LSPApplicationStateImpl {
        return this
    }

    override fun loadState(lspState: LSPApplicationStateImpl) {
        try {
            XmlSerializerUtil.copyBean(lspState, this)
            logger.info("LSP Application State loaded")
            if (timeouts.isNotEmpty()) {
                Timeout.timeouts = timeouts
            }
        } catch (e: Exception) {
            logger.warn("Couldn't load LSP Application State : $e")
            ApplicationUtils.invokeLater {
                Messages.showErrorDialog(
                    "Couldn't load LSP settings, you will need to reconfigure them.",
                    "LSP plugin"
                )
            }
        }
    }

    companion object {
        private val logger = Logger.getInstance(LSPApplicationStateImpl::class.java)
    }
}