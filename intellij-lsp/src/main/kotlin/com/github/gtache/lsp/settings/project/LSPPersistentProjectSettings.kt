package com.github.gtache.lsp.settings.project

import com.intellij.openapi.components.PersistentStateComponent

/**
 * Represents LSP project settings
 */
interface LSPPersistentProjectSettings : PersistentStateComponent<LSPProjectState> {
    /**
     * The current project settings state
     */
    var projectState: LSPProjectState


    /**
     * Adds a [listener] which will listen to the state changes
     */
    fun addListener(listener: Listener)

    /**
     * Removes a [listener]
     */
    fun removeListener(listener: Listener)

    /**
     * Returns all the listeners listening to these settings
     */
    fun getListeners(): Set<Listener>

    /**
     * Listener of these settings
     */
    interface Listener {
        /**
         * Notifies that the [oldState] has been replaced by the [newState]
         */
        fun stateChanged(oldState: LSPProjectState, newState: LSPProjectState)
    }
}