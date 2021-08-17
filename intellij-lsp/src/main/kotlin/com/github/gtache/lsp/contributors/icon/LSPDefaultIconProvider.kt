package com.github.gtache.lsp.contributors.icon

import com.github.gtache.lsp.client.languageserver.status.ServerStatus
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.SymbolKind
import javax.swing.Icon


object LSPDefaultIconProvider : LSPIconProvider {

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

    override fun isSpecificFor(serverDefinition: LanguageServerDefinition): Boolean = false
}