package com.github.gtache.lsp.services.project

import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapperImpl
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j.SymbolKind

/**
 * Represents an LSP service at the project level
 */
interface LSPProjectService {
    /**
     * Map of file extension to server definition
     */
    var extensionsToServerDefinitions: Map<String, LanguageServerDefinition>

    /**
     * Returns whether there is a server definition supporting the given [extension]
     */
    fun isExtensionSupported(extension: String?): Boolean {
        return extension != null && extensionsToServerDefinitions.contains(extension)
    }

    /**
     * Returns the corresponding workspaceSymbols given a [name], an unused [pattern], whether to search in non-project items [includeNonProjectItems] unused and a kind set [onlyKind] for filtering
     *
     * @param name                   The name to search for
     * @param pattern                The pattern (unused)
     * @param includeNonProjectItems Whether to search in libraries for example (unused)
     * @param onlyKind               Filter the results to only the kinds in the set (all by default)
     * Returns An array of NavigationItem
     */
    fun workspaceSymbols(
        name: String,
        pattern: String,
        includeNonProjectItems: Boolean = false,
        onlyKind: Set<SymbolKind> = emptySet()
    ): Array<NavigationItem>


    /**
     * Notifies that the settings state has been loaded
     */
    fun notifyStateLoaded(): Unit

    /**
     * Returns all the wrappers currently instantiated
     */
    fun getAllWrappers(): Set<LanguageServerWrapper>

    /**
     * Removes a wrapper
     */
    fun removeWrapper(wrapper: LanguageServerWrapper): Unit

    /**
     * Called when an [editor] is opened. Instantiates a LanguageServerWrapper if necessary, and links the [editor] to this wrapper
     */
    fun editorOpened(editor: Editor): Unit

    /**
     * Called when an [editor] is closed. Notifies the LanguageServerWrapper if needed
     */
    fun editorClosed(editor: Editor): Unit {
        val uri = FileUtils.editorToURIString(editor)
        if (uri != null) {
            LanguageServerWrapperImpl.forEditor(editor)?.let { l ->
                ApplicationUtils.pool {
                    logger.info("Disconnecting $uri")
                    l.disconnect(uri)
                }
            }
        }
    }

    /**
     * Forces the linking from [editor] to the wrapper corresponding to [serverDefinition] in the given [project]
     */
    fun forceEditorLink(editor: Editor, serverDefinition: LanguageServerDefinition, project: Project): Unit

    /**
     * Reset all the custom associations
     */
    fun resetAssociations(): Unit

    companion object {
        private val logger: Logger = Logger.getInstance(LSPProjectService::class.java)
    }

}