package com.github.gtache.lsp.services.project

import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.client.languageserver.serverdefinition.UserConfigurableServerDefinition
import com.github.gtache.lsp.client.languageserver.status.ServerStatus
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapperImpl
import com.github.gtache.lsp.contributors.LSPNavigationItem
import com.github.gtache.lsp.requests.Timeout
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.settings.project.LSPProjectSettings
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.FileUtils
import com.github.gtache.lsp.utils.GUIUtils
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.WorkspaceSymbolParams
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Implementation of LSPProjectService
 */
class LSPProjectServiceImpl(private val project: Project) : LSPProjectService {

    private val extToServerWrapper: MutableMap<String, LanguageServerWrapper> = HashMap()

    private val projectSettings = project.service<LSPProjectSettings>()

    override var extensionsToServerDefinitions: Map<String, LanguageServerDefinition> = emptyMap()
        get() {
            return field.toMap()
        }
        set(value) {
            val oldServerDef = field
            val flattened = value.flatMap { t -> t.key.split(LanguageServerDefinition.SPLIT_CHAR).map { ext -> Pair(ext, t.value) } }.toMap()
            field = flattenExt(flattened)
            ApplicationUtils.pool {
                val modified = flattened.map { servdef ->
                    if (oldServerDef.contains(servdef.key) && oldServerDef[servdef.key] != servdef.value) {
                        servdef.key
                    } else {
                        null
                    }
                }.filterIsInstance<String>().toSet()
                val added = flattened.keys.filter { e -> !oldServerDef.contains(e) }.toSet() + modified
                val removed = oldServerDef.keys.filter { e -> !flattened.contains(e) }.toSet() + modified
                synchronized(forcedAssociations) {
                    synchronized(forcedAssociationsInstances) {
                        val newValues = flattened.values.toSet()
                        forcedAssociations.filter { t -> !newValues.contains(t.value) }.forEach { forcedAssociations.remove(it.key) }
                        forcedAssociationsInstances.filter { t -> !newValues.contains(t.value.serverDefinition) }.keys.forEach { k ->
                            forcedAssociationsInstances[k]?.disconnect(k)
                            forcedAssociationsInstances.remove(k)
                        }
                    }
                }
                extToServerWrapper.keys.filter { k -> removed.contains(k) }.forEach { k ->
                    val wrapper = extToServerWrapper[k]
                    wrapper?.stop()
                    wrapper?.removeWidget()
                    extToServerWrapper.remove(k)
                }
                val openedEditors: Iterable<Editor> = ApplicationUtils.computableReadAction {
                    ProjectManager.getInstance().openProjects.flatMap { proj -> FileEditorManager.getInstance(proj).allEditors.toList() }
                        .filterIsInstance<TextEditor>().map { t -> t.editor }
                }
                val files = openedEditors.map { e -> FileDocumentManager.getInstance().getFile(e.document) }
                files.zip(openedEditors).forEach { f -> if (added.contains(f.first?.extension)) editorOpened(f.second) }
            }
        }
    private val forcedAssociations: MutableMap<String, LanguageServerDefinition> = HashMap()

    private var loadedExtensions: Boolean = false
    private val forcedAssociationsInstances: MutableMap<String, LanguageServerWrapper> = HashMap(10)

    companion object {
        private val logger: Logger = Logger.getInstance(LSPProjectServiceImpl::class.java)
    }

    init {
        project.service<LSPProjectSettings>()
    }

    override fun editorOpened(editor: Editor): Unit {
        if (editor.project == project) {
            addExtensions()
            val file: VirtualFile? = FileDocumentManager.getInstance().getFile(editor.document)
            if (file != null) {
                ApplicationUtils.pool {
                    val ext: String? = file.extension
                    logger.info("Opened " + file.name)
                    val uri = FileUtils.editorToURIString(editor)
                    val pUri = FileUtils.editorToProjectFolderUri(editor)
                    if (uri != null && pUri != null && ext != null) {
                        val forced = forcedAssociationsInstances[uri]
                        if (forced == null) {
                            val forcedDef = forcedAssociations[uri]
                            if (forcedDef == null) {
                                val serverDefinition = extensionsToServerDefinitions[ext]
                                serverDefinition?.let {
                                    val wrapper = getWrapperFor(ext, editor, it)
                                    wrapper?.let { w ->
                                        logger.info("Adding file " + file.name)
                                        w.connect(editor)
                                    }
                                }
                            } else {
                                val wrapper = getWrapperFor(ext, editor, forcedDef)
                                wrapper?.let {
                                    logger.info("Adding file " + file.name)
                                    wrapper.connect(editor)
                                }
                            }
                        } else forced.connect(editor)
                    }
                }
            } else {
                logger.warn("File for editor " + editor.document.text + " is null")
            }
        } else {
            logger.warn("Wrong editor project ${editor.project} (!= $project)")
        }
    }


    override fun forceEditorLink(editor: Editor, serverDefinition: LanguageServerDefinition, project: Project): Unit {
        addExtensions()
        val file: VirtualFile? = FileDocumentManager.getInstance().getFile(editor.document)
        if (file != null) {
            val uri = FileUtils.editorToURIString(editor)
            val pUri = FileUtils.projectToUri(project)
            if (uri != null && pUri != null) {
                synchronized(forcedAssociations) {
                    forcedAssociations[uri] = serverDefinition
                }
                ApplicationUtils.pool {
                    LanguageServerWrapperImpl.forEditor(editor)?.let { l ->
                        logger.info("Disconnecting ${FileUtils.editorToURIString(editor)}")
                        l.disconnect(editor)
                    }
                    logger.info("Opened ${file.name}" + file.name)
                    val wrapper = getWrapperFor(serverDefinition.ext, editor, serverDefinition)
                    wrapper?.let {
                        projectSettings.projectState = projectSettings.projectState.withForcedAssociations(forcedAssociations.map { mapping ->
                            mapping.key to mapping.value.toArray()
                        }.toMap())
                        it.connect(editor)
                        logger.info("Adding file " + file.name)
                    }
                }
            }
        } else {
            logger.warn("File for editor ${editor.document.text} is null")
        }
    }


    private fun getWrapperFor(ext: String, editor: Editor, serverDefinition: LanguageServerDefinition): LanguageServerWrapper? {
        if (editor.project == project && !project.isDefault) {
            val rootVFS: VirtualFile? = project.guessProjectDir()
            if (rootVFS == null) {
                val docName = FileDocumentManager.getInstance().getFile(editor.document)?.canonicalPath
                logger.warn("Null rootPath for $docName")
                Messages.showErrorDialog(project, "Can't infer project directory from project\nThe plugin won't work for $docName", "LSP error")
                return null
            } else {
                val rootPath = FileUtils.vfsToPath(rootVFS)
                val rootUri = FileUtils.pathToUri(rootPath)
                synchronized(forcedAssociationsInstances) {
                    var wrapper = forcedAssociationsInstances[FileUtils.editorToURIString(editor)]
                    if (wrapper == null || wrapper.serverDefinition != serverDefinition) {
                        synchronized(extToServerWrapper) {
                            wrapper = extToServerWrapper[ext]
                            if (wrapper == null) {
                                logger.info("Instantiating wrapper for $ext : $rootUri")
                                wrapper = LanguageServerWrapperImpl(serverDefinition, project)
                                val exts = serverDefinition.ext.split(LanguageServerDefinition.SPLIT_CHAR)
                                exts.forEach { ext -> extToServerWrapper[ext] = wrapper!! }
                                extToServerWrapper[serverDefinition.ext] = wrapper!!
                            } else {
                                logger.info("Wrapper already existing for $ext , $rootUri")
                            }
                        }
                        synchronized(forcedAssociations) {
                            forcedAssociations.forEach { t ->
                                if (t.value == serverDefinition && t.key == rootUri) {
                                    forcedAssociationsInstances[t.key] = wrapper!!
                                }
                            }
                            val uri = FileUtils.editorToURIString(editor)
                            if (uri != null) {
                                forcedAssociationsInstances[uri] = wrapper!!
                            } else {
                                logger.warn("Null uri for $editor")
                            }
                        }
                        return wrapper
                    } else return wrapper
                }
            }
        } else {
            logger.warn("Trying to get wrapper for invalid project ${editor.project} (!=$project)")
            return null
        }
    }

    override fun removeWrapper(wrapper: LanguageServerWrapper): Unit {
        wrapper.project.basePath?.let {
            extToServerWrapper -= wrapper.serverDefinition.ext
        }
    }

    override fun workspaceSymbols(name: String, pattern: String, includeNonProjectItems: Boolean, onlyKind: Set<SymbolKind>): Array<NavigationItem> {
        val params = WorkspaceSymbolParams(name)
        val servDefToReq = extToServerWrapper.values.filter { w -> w.status == ServerStatus.STARTED && w.requestManager != null }
            .map { w -> Pair(w, w.requestManager?.symbol(params)) }.toSet().filter { w -> w.second != null }
        val servDefToSymb = servDefToReq.map { w ->
            val server = w.first
            val symbolF = w.second!!
            try {
                server.notifyResult(Timeouts.SYMBOLS, success = true)
                Pair(
                    server,
                    symbolF.get(Timeout.SYMBOLS_TIMEOUT(), TimeUnit.MILLISECONDS)
                        ?.filter { s -> if (onlyKind.isEmpty()) true else onlyKind.contains(s.kind) })
            } catch (e: TimeoutException) {
                logger.warn(e)
                server.notifyResult(Timeouts.SYMBOLS, success = false)
                null
            }
        }.filterNotNull().filter { r -> r.second != null }
        return servDefToSymb.flatMap { res ->
            val definition = res.first
            val symbols = res.second!!
            symbols.mapNotNull { symb ->
                val start = symb.location.range.start
                val uri = FileUtils.uriToVFS(symb.location.uri)
                if (uri != null) {
                    val iconProvider = GUIUtils.getIconProviderFor(definition.serverDefinition)
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
        }.toTypedArray()
    }

    override fun notifyStateLoaded() {
        val state = project.service<LSPProjectSettings>().projectState
        extensionsToServerDefinitions = state.extensionsToServers.mapValues { e -> UserConfigurableServerDefinition.fromArray(e.value) }
            .filterValues { e -> e != null }.mapValues { e -> e.value!! }
        forcedAssociations.clear()
        forcedAssociations.putAll(state.forcedAssociations.mapValues { e -> UserConfigurableServerDefinition.fromArray(e.value) }
            .filterValues { e -> e != null }.mapValues { e -> e.value!! })
    }

    private fun addExtensions(): Unit {
        if (!loadedExtensions) {
            val extensions = LanguageServerDefinition.ALL_DEFINITIONS.filter { s -> !extensionsToServerDefinitions.contains(s.ext) }
            logger.info("Added serverDefinitions $extensions from plugins")
            extensionsToServerDefinitions = extensionsToServerDefinitions + extensions.map { s -> Pair(s.ext, s) }
            extensionsToServerDefinitions = flattenExt(extensionsToServerDefinitions)
            loadedExtensions = true
        }
    }

    private fun flattenExt(map: Map<String, LanguageServerDefinition>): Map<String, LanguageServerDefinition> {
        return map.map { p ->
            val ext = p.key
            val sDef = p.value
            val split = ext.split(LanguageServerDefinition.SPLIT_CHAR)
            split.map { s -> Pair(s, sDef) } + Pair(ext, sDef)
        }.flatten().toMap()
    }

    override fun resetAssociations(): Unit {
        synchronized(forcedAssociationsInstances) {
            forcedAssociationsInstances.forEach { t -> t.value.disconnect(t.key) }
            forcedAssociationsInstances.clear()
        }
        synchronized(forcedAssociations) {
            forcedAssociations.clear()
            projectSettings.projectState = projectSettings.projectState.withForcedAssociations(emptyMap())
        }
    }

    override fun getAllWrappers(): Set<LanguageServerWrapper> {
        return extToServerWrapper.values.toSet()
    }

}