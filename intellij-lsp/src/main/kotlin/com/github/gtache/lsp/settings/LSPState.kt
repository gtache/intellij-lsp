package com.github.gtache.lsp.settings

import com.github.gtache.lsp.PluginMain
import com.github.gtache.lsp.client.languageserver.serverdefinition.UserConfigurableServerDefinition
import com.github.gtache.lsp.requests.Timeout
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.utils.ApplicationUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.util.xmlb.XmlSerializerUtil
import java.util.*
import kotlin.collections.HashMap

@State(name = "LSPState", storages = [Storage(value = "LSPState.xml")])
/**
 * Class representing the state of the LSP settings
 */
class LSPState : PersistentStateComponent<LSPState> {
    //Must be public to be saved
    var isLoggingServersOutput = false
    var isAlwaysSendRequests = false
    var extToServ: Map<String, Array<String>> = LinkedHashMap(10)
        get() = field.mapValues { it.value.copyOf() }.toMap()
        set(value) {
            field = value.mapValues { it.value.copyOf() }.toMap()
        }
    var timeouts: Map<Timeouts, Long> = HashMap(Timeouts.values().size)
        get() = field.toMap()
        set(value) {
            field = value.toMap()
        }
    var coursierResolvers: List<String> = ArrayList(5)
        get() = field.toList()
        set(value) {
            field = value.toList()
        }
    var forcedAssociations: Map<Array<String>, Array<String>> = HashMap(10)
        get() = field.toMap()
        set(value) {
            field = value.toMap()
        }

    fun setLogServersOutput(b: Boolean) {
        isLoggingServersOutput = b
    }

    override fun hashCode(): Int {
        return Objects.hash(isLoggingServersOutput, isAlwaysSendRequests, extToServ, timeouts, coursierResolvers, forcedAssociations)
    }

    override fun equals(other: Any?): Boolean {
        return (other is LSPState) && isLoggingServersOutput == other.isLoggingServersOutput && isAlwaysSendRequests == other.isAlwaysSendRequests && extToServ == other.extToServ && timeouts == other.timeouts && coursierResolvers == other.coursierResolvers && forcedAssociations == other.forcedAssociations
    }

    override fun getState(): LSPState {
        return this
    }

    override fun loadState(lspState: LSPState) {
        try {
            XmlSerializerUtil.copyBean(lspState, this)
            logger.info("LSP State loaded")
            PluginMain.setExtToServerDefinition(UserConfigurableServerDefinition.fromArrayMap(extToServ))
            if (timeouts.isNotEmpty()) {
                Timeout.timeouts = timeouts
            }
            PluginMain.setForcedAssociations(forcedAssociations)
        } catch (e: Exception) {
            logger.warn("Couldn't load LSPState : $e")
            ApplicationUtils.invokeLater {
                Messages.showErrorDialog(
                    "Couldn't load LSP settings, you will need to reconfigure them.",
                    "LSP plugin"
                )
            }
        }
    }

    companion object {
        private val logger = Logger.getInstance(LSPState::class.java)
        @JvmStatic
        val instance: LSPState? = try {
            ApplicationManager.getApplication().getService(LSPState::class.java)
        } catch (e: Exception) {
            logger.warn("Couldn't load LSPState : $e")
            ApplicationUtils.invokeLater {
                Messages.showErrorDialog(
                    "Couldn't load LSP settings, you will need to reconfigure them.",
                    "LSP plugin"
                )
            }
            null
        }
    }
}