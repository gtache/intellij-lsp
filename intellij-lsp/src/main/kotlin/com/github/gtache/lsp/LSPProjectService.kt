package com.github.gtache.lsp

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

interface LSPProjectService {
    var extToServerDefinition: Map<String, LanguageServerDefinition>

    /**
     * @param ext An extension
     * @return True if there is a LanguageServer supporting this extension, false otherwise
     */
    fun isExtensionSupported(ext: String?): Boolean {
        return ext != null && extToServerDefinition.contains(ext)
    }

    /**
     * Returns the corresponding workspaceSymbols given a name
     *
     * @param name                   The name to search for
     * @param pattern                The pattern (unused)
     * @param includeNonProjectItems Whether to search in libraries for example (unused)
     * @param onlyKind               Filter the results to only the kinds in the set (all by default)
     * @return An array of NavigationItem
     */
    fun workspaceSymbols(
        name: String,
        pattern: String,
        includeNonProjectItems: Boolean = false,
        onlyKind: Set<SymbolKind> = emptySet()
    ): Array<NavigationItem>


    fun notifyStateLoaded(): Unit
    fun getAllWrappers(): Set<LanguageServerWrapper>

    fun removeWrapper(wrapper: LanguageServerWrapper): Unit

    /**
     * Called when an editor is opened. Instantiates a LanguageServerWrapper if necessary, and adds the Editor to the Wrapper
     *
     * @param editor the editor
     */
    fun editorOpened(editor: Editor): Unit

    /**
     * Called when an editor is closed. Notifies the LanguageServerWrapper if needed
     *
     * @param editor the editor.
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

    fun forceEditorOpened(editor: Editor, serverDefinition: LanguageServerDefinition, project: Project): Unit

    fun resetAssociations(): Unit

    companion object {
        private val logger: Logger = Logger.getInstance(LSPProjectService::class.java)
    }

}