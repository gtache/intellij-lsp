package com.github.gtache.lsp

import com.github.gtache.lsp.client.languageserver.ServerStatus
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapperImpl
import com.github.gtache.lsp.contributors.LSPNavigationItem
import com.github.gtache.lsp.requests.Timeout
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.settings.LSPState
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.ApplicationUtils.pool
import com.github.gtache.lsp.utils.FileUtils
import com.github.gtache.lsp.utils.GUIUtils
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.components.ApplicationComponent
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
 * The main class of the plugin
 */
class PluginMain : ApplicationComponent {


    override fun initComponent(): Unit {
        LSPState.instance?.state //Need that to trigger loadState
        logger.info("PluginMain init finished")
    }

    companion object {
        private val logger: Logger = Logger.getInstance(PluginMain::class.java)
        private val extToLanguageWrapper: MutableMap<Pair<String, String>, LanguageServerWrapper> = HashMap(10)
        private val projectToLanguageWrappers: MutableMap<String, MutableSet<LanguageServerWrapper>> = HashMap(10)
        private val forcedAssociationsInstances: MutableMap<Pair<String, String>, LanguageServerWrapper> = HashMap(10)
        private var forcedAssociations: MutableMap<Pair<String, String>, LanguageServerDefinition> = HashMap(10)
        private var extToServerDefinition: Map<String, LanguageServerDefinition> = HashMap(10)
        private var loadedExtensions: Boolean = false

        @JvmStatic
        fun resetAssociations(): Unit {
            synchronized(forcedAssociationsInstances) {
                forcedAssociationsInstances.forEach { t -> t.value.disconnect(t.key.first) }
                forcedAssociationsInstances.clear()
            }
            synchronized(forcedAssociations) {
                forcedAssociations.clear()
                LSPState.instance?.forcedAssociations = forcedAssociations.map { mapping ->
                    Pair(arrayOf(mapping.key.first, mapping.key.second), mapping.value.toArray())
                }.toMap()
            }
        }

        /**
         * @return All instantiated ServerWrappers
         */
        fun getAllServerWrappers(): Set<LanguageServerWrapper> {
            return projectToLanguageWrappers.values.flatten().toSet()
        }

        /**
         * @param ext An extension
         * @return True if there is a LanguageServer supporting this extension, false otherwise
         */
        fun isExtensionSupported(ext: String?): Boolean {
            return ext != null && extToServerDefinition.contains(ext)
        }

        /**
         * Sets the extensions->languageServer mapping
         *
         * @param newExt a Scala map
         */
        fun setExtToServerDefinition(newExt: Map<String, LanguageServerDefinition>): Unit {
            val nullDef = newExt.filter { d -> d.value == null }
            val oldServerDef = extToServerDefinition
            val flattened = newExt.filter { d -> d.value != null }
                .flatMap { t -> t.key.split(LanguageServerDefinition.SPLIT_CHAR).map { ext -> Pair(ext, t.value) } }.toMap()
            extToServerDefinition = flattened
            flattenExt()
            nullDef.forEach { ext -> logger.error("Definition for $ext is null") }
            pool {
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
                        forcedAssociations = forcedAssociations.filter { t -> newValues.contains(t.value) }.toMutableMap()
                        forcedAssociationsInstances.filter { t -> !newValues.contains(t.value.serverDefinition) }.keys.forEach { k ->
                            forcedAssociationsInstances[k]?.disconnect(k.second)
                            forcedAssociationsInstances.remove(k)
                        }
                    }
                }
                extToLanguageWrapper.keys.filter { k -> removed.contains(k.first) }.forEach { k ->
                    val wrapper = extToLanguageWrapper[k]
                    wrapper?.stop()
                    wrapper?.removeWidget()
                    extToLanguageWrapper.remove(k)
                }
                val openedEditors: Iterable<Editor> = ApplicationUtils.computableReadAction {
                    ProjectManager.getInstance().openProjects.flatMap { proj -> FileEditorManager.getInstance(proj).allEditors.toList() }
                        .filterIsInstance<TextEditor>().map { t -> t.editor }
                }
                val files = openedEditors.map { e -> FileDocumentManager.getInstance().getFile(e.document) }
                files.zip(openedEditors).forEach { f -> if (added.contains(f.first?.extension)) editorOpened(f.second) }
            }
        }

        /**
         * Called when an editor is opened. Instantiates a LanguageServerWrapper if necessary, and adds the Editor to the Wrapper
         *
         * @param editor the editor
         */
        fun editorOpened(editor: Editor): Unit {
            addExtensions()
            val file: VirtualFile? = FileDocumentManager.getInstance().getFile(editor.document)
            if (file != null) {
                pool {
                    val ext: String? = file.extension
                    logger.info("Opened " + file.name)
                    val uri = FileUtils.editorToURIString(editor)
                    val pUri = FileUtils.editorToProjectFolderUri(editor)
                    if (uri != null && pUri != null && ext != null) {
                        val forced = forcedAssociationsInstances[Pair(uri, pUri)]
                        if (forced == null) {
                            val forcedDef = forcedAssociations[Pair(uri, pUri)]
                            if (forcedDef == null) {
                                val serverDefinition = extToServerDefinition[ext]
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
        }

        fun forceEditorOpened(editor: Editor, serverDefinition: LanguageServerDefinition, project: Project): Unit {
            addExtensions()
            val file: VirtualFile? = FileDocumentManager.getInstance().getFile(editor.document)
            if (file != null) {
                val uri = FileUtils.editorToURIString(editor)
                val pUri = FileUtils.projectToUri(project)
                if (uri != null && pUri != null) {
                    synchronized(forcedAssociations) {
                        forcedAssociations.put(Pair(uri, pUri), serverDefinition)
                    }
                    pool {
                        LanguageServerWrapperImpl.forEditor(editor)?.let { l ->
                            logger.info("Disconnecting " + FileUtils.editorToURIString(editor))
                            l.disconnect(editor)
                        }
                        logger.info("Opened " + file.name)
                        val wrapper = getWrapperFor(serverDefinition.ext, editor, serverDefinition)
                        wrapper?.let {
                            LSPState.instance?.forcedAssociations = forcedAssociations.map { mapping ->
                                arrayOf(
                                    mapping.key.first,
                                    mapping.key.second
                                ) to mapping.value.toArray()
                            }.toMap()
                            it.connect(editor)
                            logger.info("Adding file " + file.name)
                        }
                    }
                }
            } else {
                logger.warn("File for editor " + editor.document.text + " is null")
            }
        }

        private fun getWrapperFor(ext: String, editor: Editor, serverDefinition: LanguageServerDefinition): LanguageServerWrapper? {
            val project: Project? = editor.project
            if (project != null && !project.isDefault) {
                val rootVFS: VirtualFile? = project.guessProjectDir()
                if (rootVFS == null) {
                    val docName = FileDocumentManager.getInstance().getFile(editor.document)?.canonicalPath
                    logger.warn("Null rootPath for $docName")
                    Messages.showErrorDialog(project, "Can't infer project directory from project\nThe plugin won't work for $docName", "LSP error")
                    return null
                } else {
                    val rootPath = FileUtils.VFSToPath(rootVFS)
                    val rootUri = FileUtils.pathToUri(rootPath)
                    if (rootUri != null) {
                        synchronized(forcedAssociationsInstances) {
                            var wrapper = forcedAssociationsInstances[Pair(FileUtils.editorToURIString(editor), FileUtils.projectToUri(project))]
                            if (wrapper == null || wrapper.serverDefinition != serverDefinition) {
                                synchronized(extToLanguageWrapper) {
                                    wrapper = extToLanguageWrapper[Pair(ext, rootUri)]
                                    if (wrapper == null) {
                                        logger.info("Instantiating wrapper for $ext : $rootUri")
                                        wrapper = LanguageServerWrapperImpl(serverDefinition, project)
                                        val exts = serverDefinition.ext.split(LanguageServerDefinition.SPLIT_CHAR)
                                        exts.forEach { ext -> extToLanguageWrapper[Pair(ext, rootUri)] = wrapper!! }
                                        extToLanguageWrapper[Pair(serverDefinition.ext, rootUri)] = wrapper!!
                                        val wrapperSet = projectToLanguageWrappers[rootUri]
                                        if (wrapperSet != null) {
                                            wrapperSet.add(wrapper!!)
                                        } else {
                                            projectToLanguageWrappers[rootUri] = mutableSetOf(wrapper!!)
                                        }
                                    } else {
                                        logger.info("Wrapper already existing for $ext , $rootUri")
                                    }
                                }
                                synchronized(forcedAssociations) {
                                    forcedAssociations.forEach { t ->
                                        if (t.value == serverDefinition && t.key.first == rootUri) {
                                            forcedAssociationsInstances[t.key] = wrapper!!
                                        }
                                    }
                                    val uri = FileUtils.editorToURIString(editor)
                                    if (uri != null) {
                                        forcedAssociationsInstances[Pair(uri, rootUri)] = wrapper!!
                                    } else {
                                        logger.warn("Null uri for $editor")
                                    }
                                }
                                return wrapper
                            } else return wrapper
                        }
                    } else {
                        logger.warn("rootURI is null for $rootPath")
                        return null
                    }
                }
            } else return null
        }

        /**
         * Returns the extensions->languageServer mapping
         *
         */
        fun getExtToServerDefinition(): Map<String, LanguageServerDefinition> {
            addExtensions()
            return extToServerDefinition
        }

        private fun addExtensions(): Unit {
            if (!loadedExtensions) {
                val extensions = LanguageServerDefinition.allDefinitions.filter { s -> !extToServerDefinition.contains(s.ext) }
                logger.info("Added serverDefinitions $extensions from plugins")
                extToServerDefinition = extToServerDefinition + extensions.map { s -> Pair(s.ext, s) }
                flattenExt()
                loadedExtensions = true
            }
        }

        private fun flattenExt(): Unit {
            extToServerDefinition = extToServerDefinition.map { p ->
                val ext = p.key
                val sDef = p.value
                val split = ext.split(LanguageServerDefinition.SPLIT_CHAR)
                split.map { s -> Pair(s, sDef) } + Pair(ext, sDef)
            }.flatten().toMap()
        }

        /**
         * Called when an editor is closed. Notifies the LanguageServerWrapper if needed
         *
         * @param editor the editor.
         */
        fun editorClosed(editor: Editor): Unit {
            val uri = FileUtils.editorToURIString(editor)
            if (uri != null) {
                LanguageServerWrapperImpl.forEditor(editor)?.let { l ->
                    pool {
                        logger.info("Disconnecting $uri")
                        l.disconnect(uri)
                    }
                }
            }
        }


        /**
         * Returns the corresponding workspaceSymbols given a name and a project
         *
         * @param name                   The name to search for
         * @param pattern                The pattern (unused)
         * @param project                The project in which to search
         * @param includeNonProjectItems Whether to search in libraries for example (unused)
         * @param onlyKind               Filter the results to only the kinds in the set (all by default)
         * @return An array of NavigationItem
         */
        fun workspaceSymbols(
            name: String,
            pattern: String,
            project: Project,
            includeNonProjectItems: Boolean = false,
            onlyKind: Set<SymbolKind> = emptySet()
        ): Array<NavigationItem> {
            val wrapperSet = if (project.basePath != null) projectToLanguageWrappers[FileUtils.pathToUri(project.basePath!!)] else null
            if (wrapperSet != null) {
                val params = WorkspaceSymbolParams(name)
                val servDefToReq = wrapperSet.filter { w -> w.status == ServerStatus.STARTED && w.requestManager != null }
                    .map { w -> Pair(w, w.requestManager?.symbol(params)) }.toSet().filter { w -> w.second != null }
                if (servDefToReq.none { it == null }) {
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
                            val uri = FileUtils.URIToVFS(symb.location.uri)
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

                } else return emptyArray()
            } else {
                logger.info("No wrapper for project " + project.basePath)
                return emptyArray()
            }
        }

        fun removeWrapper(wrapper: LanguageServerWrapper): Unit {
            wrapper.project.basePath?.let {
                extToLanguageWrapper.remove(Pair(wrapper.serverDefinition.ext, FileUtils.pathToUri(it)))
            }
        }

        fun setForcedAssociations(associations: Map<Array<String>, Array<String>>): Unit {
            if (!associations.keys.all { t -> t.size == 2 }) {
                logger.warn("Unable to set forced associations : bad array length")
            } else {
                this.forcedAssociations = associations.map { mapping ->
                    Pair(
                        Pair(mapping.key[0], mapping.value[1]),
                        LanguageServerDefinition.fromArray(mapping.value)
                    )
                }.toMap().filterValues { it != null }.mapValues { it.value!! }.toMutableMap()
            }
        }
    }
}