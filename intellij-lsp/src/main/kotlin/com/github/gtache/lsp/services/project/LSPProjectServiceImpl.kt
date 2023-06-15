package com.github.gtache.lsp.services.project

import com.github.gtache.lsp.contributors.LSPNavigationItem
import com.github.gtache.lsp.contributors.icon.LanguageServerIconProvider
import com.github.gtache.lsp.languageserver.definition.Definition
import com.github.gtache.lsp.languageserver.status.ServerStatus
import com.github.gtache.lsp.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.languageserver.wrapper.provider.LanguageServerWrapperProvider
import com.github.gtache.lsp.requests.Timeout
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.settings.project.LSPPersistentProjectSettings
import com.github.gtache.lsp.settings.project.LSPProjectState
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.WorkspaceSymbolParams
import java.util.concurrent.*
import kotlin.streams.toList

/**
 * Implementation of LSPProjectService
 */
class LSPProjectServiceImpl(private val project: Project) : LSPProjectService {

    private val extensionToServerWrapper: ConcurrentMap<String, LanguageServerWrapper> = ConcurrentHashMap()
    private val uriToServerWrapper: ConcurrentMap<String, LanguageServerWrapper> = ConcurrentHashMap()

    private val forcedAssociationsInstances: ConcurrentMap<String, LanguageServerWrapper> = ConcurrentHashMap(10)

    private val projectSettings = project.service<LSPPersistentProjectSettings>()

    companion object {
        private val logger: Logger = Logger.getInstance(LSPProjectServiceImpl::class.java)
    }

    init {
        projectSettings.addListener(this)
    }

    override fun editorOpened(editor: Editor): Unit {
        if (editor.project == project) {
            ApplicationUtils.pool {
                val ext = FileUtils.editorToExtension(editor)
                val uri = FileUtils.editorToUri(editor)
                if (uri != null && ext != null) {
                    logger.info("Opened $uri")
                    val forcedDef = getState().forcedAssociations[uri]
                    if (forcedDef == null) {
                        val serverDefinition = getDefinitionForExtension(ext)
                        serverDefinition?.let {
                            val wrapper = project.service<LanguageServerWrapperProvider>().getWrapper(it)
                            serverDefinition.extensions.forEach { ext -> extensionToServerWrapper[ext] = wrapper }
                            wrapper.connect(editor)
                            uriToServerWrapper[uri] = wrapper
                            logger.info("Connected $uri")
                        }
                    } else {
                        getState().extensionToServerDefinition[forcedDef]?.let { def ->
                            forceEditorLink(editor, def)
                        }
                    }
                }
            }
        } else {
            logger.error("Wrong editor project ${editor.project} != $project")
        }
    }

    override fun editorClosed(editor: Editor) {
        if (editor.project == project) {
            getWrapper(editor)?.disconnect(editor)
            FileUtils.editorToUri(editor)?.let {
                uriToServerWrapper.remove(it)
                forcedAssociationsInstances.remove(it)
            }
        } else {
            logger.error("Wrong editor project ${editor.project} != $project")
        }
    }

    override fun forceEditorLink(editor: Editor, serverDefinition: Definition): Unit {
        if (editor.project == project) {
            val uri = FileUtils.editorToUri(editor)
            if (uri != null) {
                if (getState().forcedAssociations[uri] != serverDefinition.id) {
                    projectSettings.projectState = projectSettings.projectState.withForcedAssociations(getState().forcedAssociations.plus(uri to serverDefinition.id))
                }
                ApplicationUtils.pool {
                    getWrapper(editor)?.let { l ->
                        uriToServerWrapper.remove(uri)
                        l.disconnect(editor)
                        logger.info("Disconnected $uri")
                    }
                    logger.info("Opened $uri")
                    val wrapper = project.service<LanguageServerWrapperProvider>().getWrapper(serverDefinition)
                    serverDefinition.extensions.forEach { ext -> extensionToServerWrapper[ext] = wrapper }
                    uriToServerWrapper[uri] = wrapper
                    forcedAssociationsInstances[uri] = wrapper
                    wrapper.connect(editor)
                    logger.info("Connected $uri")
                }
            } else {
                logger.warn("Null uri for $editor")
            }
        } else {
            logger.error("Wrong editor project ${editor.project} != $project")
        }
    }

    override fun isExtensionSupported(extension: String?): Boolean {
        return projectSettings.projectState.extensionToServerDefinition.containsKey(extension) ||
                Definition.EP_NAME.extensions.flatMap { d -> d.extensions }.contains(extension)
    }

    override fun workspaceSymbols(name: String, pattern: String, includeNonProjectItems: Boolean, onlyKind: Set<SymbolKind>): List<NavigationItem> {
        val params = WorkspaceSymbolParams(name)
        val servDefToReq = extensionToServerWrapper.values.filter { w -> w.status == ServerStatus.STARTED && w.requestManager != null }
            .map { w -> Pair(w, w.requestManager?.symbol(params) ?: CompletableFuture.completedFuture(emptyList())) }.toSet().filter { w -> w.second != null }
        val servDefToSymb = servDefToReq.mapNotNull { (server, second) ->
            try {
                server.notifyResult(Timeouts.SYMBOLS, success = true)
                Pair(
                    server,
                    second.get(Timeout.SYMBOLS_TIMEOUT(), TimeUnit.MILLISECONDS)
                        ?.filter { s -> onlyKind.isEmpty() || onlyKind.contains(s.kind) } ?: emptyList<SymbolInformation>())
            } catch (e: TimeoutException) {
                logger.warn(e)
                server.notifyResult(Timeouts.SYMBOLS, success = false)
                null
            }
        }
        return servDefToSymb.flatMap { (definition, second) ->
            second.mapNotNull { symb ->
                val start = symb.location.range.start
                val uri = FileUtils.uriToVFS(symb.location.uri)
                if (uri != null) {
                    val iconProvider = LanguageServerIconProvider.getProviderFor(definition.serverDefinition)
                    LSPNavigationItem(
                        symb.name,
                        symb.containerName,
                        project,
                        uri,
                        start.line,
                        start.character,
                        iconProvider.getSymbolIcon(symb.kind)
                    )
                } else null
            }
        }
    }

    override fun resetAssociations(): Unit {
        forcedAssociationsInstances.forEach { (uri, wrapper) ->
            wrapper.disconnect(uri)
            FileUtils.uriToEditor(uri, project)?.let { e -> editorOpened(e) }
        }
        forcedAssociationsInstances.clear()
    }

    override fun stateChanged(oldState: LSPProjectState, newState: LSPProjectState) {
        manageChangedDefinitions(oldState.idToDefinition.values.toSet())
        manageChangedAssociations(oldState.forcedAssociations)
    }

    override fun getAllWrappers(): Set<LanguageServerWrapper> {
        return extensionToServerWrapper.values.toSet()
    }

    override fun getWrapper(uri: String): LanguageServerWrapper? {
        return uriToServerWrapper[uri]
    }

    private fun getState(): LSPProjectState {
        return projectSettings.projectState
    }

    private fun forceEditorLink(uri: String, serverDefinition: Definition): Unit {
        FileUtils.uriToEditor(uri, project)?.let {
            forceEditorLink(it, serverDefinition)
        }
    }

    private fun manageChangedDefinitions(oldDefinitions: Set<Definition>) {
        ApplicationUtils.pool {
            val newValues = getState().idToDefinition.values + Definition.EP_NAME.extensions().toList().filterNotNull()
            forcedAssociationsInstances.filter { t -> !newValues.contains(t.value.serverDefinition) }.keys.forEach { k ->
                forcedAssociationsInstances[k]?.disconnect(k)
                forcedAssociationsInstances.remove(k)
            }
            val modified = newValues.filter { d -> oldDefinitions.map { e -> e.id }.contains(d.id) && !oldDefinitions.contains(d) }
            val removed = oldDefinitions.toMutableSet()
            removed.removeIf { d -> newValues.map { e -> e.id }.contains(d.id) }
            removed.addAll(modified)
            val added = newValues.toMutableSet()
            added.removeIf { d -> oldDefinitions.map { e -> e.id }.contains(d.id) }
            added.addAll(modified)
            extensionToServerWrapper.filter { (_, v) -> removed.contains(v.serverDefinition) }.forEach { (k) ->
                val wrapper = extensionToServerWrapper[k]
                wrapper?.stop()
                wrapper?.removeWidget()
                extensionToServerWrapper.remove(k)
            }
            uriToServerWrapper.filter { (_, v) -> removed.contains(v.serverDefinition) }.forEach { (k) ->
                val wrapper = uriToServerWrapper[k]
                wrapper?.stop()
                wrapper?.removeWidget()
                uriToServerWrapper.remove(k)
            }
            val openedEditors = getOpenedEditors()
            val files = openedEditors.map { e -> FileDocumentManager.getInstance().getFile(e.document) }
            files.zip(openedEditors).forEach { (first, second) -> if (added.flatMap { d -> d.extensions }.contains(first?.extension)) editorOpened(second) }
        }
    }

    private fun manageChangedAssociations(oldAssociations: Map<String, String>) {
        ApplicationUtils.pool {
            val openedEditors = getOpenedEditors()
            oldAssociations.forEach { (oldUri, oldId) ->
                val id = getState().forcedAssociations[oldUri]
                if (id == null) {
                    forcedAssociationsInstances[oldUri]?.disconnect(oldUri)
                    forcedAssociationsInstances.remove(oldUri)
                } else if (id != oldId) {
                    forcedAssociationsInstances[oldUri]?.disconnect(oldUri)
                    forcedAssociationsInstances.remove(oldUri)
                    getState().idToDefinition[id]?.let { forceEditorLink(oldUri, it) }
                }
            }
            getState().forcedAssociations.forEach { (newUri, newId) ->
                if (!oldAssociations.containsKey(newUri) && openedEditors.map { e -> FileUtils.editorToUri(e) }.contains(newUri)) {
                    getState().idToDefinition[newId]?.let { forceEditorLink(newUri, it) }
                }
            }
        }
    }

    private fun getOpenedEditors(): List<Editor> {
        return ApplicationUtils.computableReadAction {
            ProjectManager.getInstance().openProjects.flatMap { proj -> FileEditorManager.getInstance(proj).allEditors.toList() }
                .filterIsInstance<TextEditor>().map { t -> t.editor }
        }
    }

    private fun getDefinitionForExtension(extension: String): Definition? {
        return getState().extensionToServerDefinition[extension] ?: Definition.EP_NAME.extensions.firstOrNull { d ->
            d.extensions.contains(extension)
        }
    }
}