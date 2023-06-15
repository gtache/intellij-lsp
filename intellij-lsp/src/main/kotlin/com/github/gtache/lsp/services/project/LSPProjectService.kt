package com.github.gtache.lsp.services.project

import com.github.gtache.lsp.languageserver.definition.Definition
import com.github.gtache.lsp.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.settings.project.LSPPersistentProjectSettings
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import org.eclipse.lsp4j.SymbolKind

/**
 * Represents an LSP service at the project level
 */
interface LSPProjectService : LSPPersistentProjectSettings.Listener {

    /**
     * Returns whether there is a server definition supporting the given [extension]
     */
    fun isExtensionSupported(extension: String?): Boolean

    /**
     * Returns the corresponding workspaceSymbols given a [name], a [pattern] (unused), whether to search in non-project items [includeNonProjectItems] (unused) and a kind set [onlyKind] for filtering
     */
    fun workspaceSymbols(
        name: String,
        pattern: String,
        includeNonProjectItems: Boolean = false,
        onlyKind: Set<SymbolKind> = emptySet()
    ): List<NavigationItem>

    /**
     * Returns all the wrappers currently instantiated
     */
    fun getAllWrappers(): Set<LanguageServerWrapper>

    /**
     * Returns the wrapper currently managing the given [editor]
     */
    fun getWrapper(editor: Editor): LanguageServerWrapper? {
        return FileUtils.editorToUri(editor)?.let { getWrapper(it) }
    }

    /**
     * Returns the wrapper currently managing the file with the given [uri]
     */
    fun getWrapper(uri: String): LanguageServerWrapper?

    /**
     * Called when an [editor] is opened. Instantiates a LanguageServerWrapper if necessary, and links the [editor] to this wrapper
     */
    fun editorOpened(editor: Editor): Unit

    /**
     * Called when an [editor] is closed. Notifies the LanguageServerWrapper if needed
     */
    fun editorClosed(editor: Editor)

    /**
     * Forces the linking from [editor] to the wrapper corresponding to [serverDefinition]
     */
    fun forceEditorLink(editor: Editor, serverDefinition: Definition): Unit

    /**
     * Reset all the custom associations
     */
    fun resetAssociations(): Unit

    companion object {
        private val logger: Logger = Logger.getInstance(LSPProjectService::class.java)
    }

}