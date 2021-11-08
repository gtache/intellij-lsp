package com.github.gtache.lsp.requests

import com.github.gtache.lsp.client.languageserver.status.ServerStatus
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.editor.services.application.EditorApplicationService
import com.github.gtache.lsp.editor.services.project.EditorProjectService
import com.github.gtache.lsp.services.project.LSPProjectService
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.FileChangeType

/**
 * Handles all file events (save, willSave, changed, etc)
 */
object FileEventManager {

    /**
     * Indicates that a document will be saved
     *
     * @param doc The document
     */
    fun willSave(doc: Document): Unit {
        service<EditorApplicationService>().managerForDocument(doc)?.willSave()
    }

    /**
     * Indicates that all documents will be saved
     */
    fun willSaveAllDocuments(): Unit {
        EditorEventManager.willSaveAll()
    }

    /**
     * Called when a file is changed. Notifies the server if this file was watched.
     *
     * @param file The file
     */
    fun fileChanged(file: VirtualFile): Unit {
        val uri = FileUtils.vfsToURI(file)
        if (uri != null) {
            val managers = ProjectManager.getInstance().openProjects.mapNotNull { p -> p.service<EditorProjectService>().forUri(uri) }
            managers.forEach { it.documentSaved() }
            if (managers.isNotEmpty()) {
                notifyServers(file, FileChangeType.Changed, managers.map { m -> m.wrapper })
            } else {
                notifyServers(file, FileChangeType.Changed)
            }
        }
    }

    /**
     * Called when a file is moved. Notifies the server if this file was watched.
     *
     * @param file The file
     */
    fun fileMoved(file: VirtualFile): Unit {

    }

    /**
     * Called when a file is deleted. Notifies the server if this file was watched.
     *
     * @param file The file
     */
    fun fileDeleted(file: VirtualFile): Unit {
        notifyServers(file, FileChangeType.Deleted)
    }

    private fun notifyServers(file: VirtualFile, typ: FileChangeType, wrappers: Collection<LanguageServerWrapper> = emptyList()): Unit {
        val uri = FileUtils.vfsToURI(file)
        if (uri != null) {
            ApplicationUtils.pool {
                val projects = ProjectManager.getInstance().openProjects.filter { p -> ProjectRootManager.getInstance(p).fileIndex.isInContent(file) }
                projects.forEach { p ->
                    p.service<LSPProjectService>().getAllWrappers().forEach { w ->
                        if (!wrappers.contains(w) && w.requestManager != null && w.status == ServerStatus.STARTED) w.didChangeWatchedFiles(uri, typ)
                    }
                }
            }
        }
    }

    /**
     * Called when a file is renamed. Notifies the server if this file was watched.
     *
     * @param oldV The old file name
     * @param newV the file name
     */
    fun fileRenamed(oldV: String, newV: String): Unit {

    }

    /**
     * Called when a file is created. Notifies the server if needed.
     *
     * @param file The file
     */
    fun fileCreated(file: VirtualFile): Unit {
        notifyServers(file, FileChangeType.Created)

    }

}