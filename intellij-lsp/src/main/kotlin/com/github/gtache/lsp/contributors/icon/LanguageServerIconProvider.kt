package com.github.gtache.lsp.contributors.icon

import com.github.gtache.lsp.head
import com.github.gtache.lsp.languageserver.definition.Definition
import com.github.gtache.lsp.languageserver.status.ServerStatus
import com.intellij.icons.AllIcons
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.IconLoader
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.SymbolKind
import javax.swing.Icon

/**
 * Represents a class providing Icons for specific language servers
 */
interface LanguageServerIconProvider {

    companion object {
        /**
         * The Extension Point name
         */
        val EP_NAME: ExtensionPointName<LanguageServerIconProvider> = ExtensionPointName.create("com.github.gtache.lsp.lspIconProvider")

        /**
         * Returns the default icon for the given completion [kind]
         */
        fun getDefaultCompletionIcon(kind: CompletionItemKind): Icon? = DefaultIconProvider.getCompletionIcon(kind)

        /**
         * Returns the default status icons
         */
        fun getDefaultStatusIcons(): Map<ServerStatus, Icon> = DefaultIconProvider.statusIcons

        /**
         * Returns the default icon for the given symbol [kind]
         */
        fun getDefaultSymbolIcon(kind: SymbolKind): Icon? = DefaultIconProvider.getSymbolIcon(kind)


        /**
         * Returns a suitable LSPIconProvider given a [serverDefinition], or the default one
         */
        fun getProviderFor(serverDefinition: Definition): LanguageServerIconProvider {
            return try {
                val providers = EP_NAME.extensions.filter { provider -> provider.canProvideFor(serverDefinition) }
                if (providers.isNotEmpty()) providers.head else DefaultIconProvider
            } catch (e: IllegalArgumentException) {
                DefaultIconProvider
            }
        }
    }

    /**
     * The server status icons
     */
    val statusIcons: Map<ServerStatus, Icon>
        get() = DefaultIconProvider.statusIcons

    /**
     * Returns the icon for the given completion [kind]
     */
    fun getCompletionIcon(kind: CompletionItemKind): Icon? = DefaultIconProvider.getCompletionIcon(kind)

    /**
     * Returns the icon for the given symbol [kind]
     */
    fun getSymbolIcon(kind: SymbolKind): Icon? = DefaultIconProvider.getSymbolIcon(kind)

    /**
     * Returns whether this provider provides specific icons for the given [serverDefinition]
     */
    fun canProvideFor(serverDefinition: Definition): Boolean

    /**
     * The default icon provider
     */
    object DefaultIconProvider : LanguageServerIconProvider {

        private val STARTED = IconLoader.getIcon("/images/started.png", javaClass)
        private val STARTING = IconLoader.getIcon("/images/starting.png", javaClass)
        private val STOPPED = IconLoader.getIcon("/images/stopped.png", javaClass)
        private val FAILED = IconLoader.getIcon("/images/failed.png", javaClass)


        override val statusIcons: Map<ServerStatus, Icon> = mapOf(
            Pair(ServerStatus.STOPPED, STOPPED),
            Pair(ServerStatus.STARTING, STARTING),
            Pair(ServerStatus.STARTED, STARTED),
            Pair(ServerStatus.FAILED, FAILED)
        )

        override fun getCompletionIcon(kind: CompletionItemKind): Icon? {
            return when (kind) {
                CompletionItemKind.Class -> AllIcons.Nodes.Class
                CompletionItemKind.Color -> null
                CompletionItemKind.Constructor -> null
                CompletionItemKind.Enum -> AllIcons.Nodes.Enum
                CompletionItemKind.Field -> AllIcons.Nodes.Field
                CompletionItemKind.File -> AllIcons.FileTypes.Any_type
                CompletionItemKind.Function -> AllIcons.Nodes.Function
                CompletionItemKind.Interface -> AllIcons.Nodes.Interface
                CompletionItemKind.Keyword -> null
                CompletionItemKind.Method -> AllIcons.Nodes.Method
                CompletionItemKind.Module -> AllIcons.Nodes.Module
                CompletionItemKind.Property -> AllIcons.Nodes.Property
                CompletionItemKind.Reference -> AllIcons.Nodes.MethodReference
                CompletionItemKind.Snippet -> null
                CompletionItemKind.Text -> AllIcons.FileTypes.Text
                CompletionItemKind.Unit -> null
                CompletionItemKind.Value -> null
                CompletionItemKind.Variable -> AllIcons.Nodes.Variable
                else -> null
            }
        }

        override fun getSymbolIcon(kind: SymbolKind): Icon? {
            return when (kind) {
                SymbolKind.Array -> null
                SymbolKind.Boolean -> null
                SymbolKind.Class -> AllIcons.Nodes.Class
                SymbolKind.Constant -> null
                SymbolKind.Constructor -> null
                SymbolKind.Enum -> AllIcons.Nodes.Enum
                SymbolKind.Field -> AllIcons.Nodes.Field
                SymbolKind.File -> AllIcons.FileTypes.Any_type
                SymbolKind.Function -> AllIcons.Nodes.Function
                SymbolKind.Interface -> AllIcons.Nodes.Interface
                SymbolKind.Method -> AllIcons.Nodes.Method
                SymbolKind.Module -> AllIcons.Nodes.Module
                SymbolKind.Namespace -> null
                SymbolKind.Number -> null
                SymbolKind.Package -> AllIcons.Nodes.Package
                SymbolKind.Property -> AllIcons.Nodes.Property
                SymbolKind.String -> null
                SymbolKind.Variable -> AllIcons.Nodes.Variable
                else -> null
            }
        }

        override fun canProvideFor(serverDefinition: Definition): Boolean = false
    }
}