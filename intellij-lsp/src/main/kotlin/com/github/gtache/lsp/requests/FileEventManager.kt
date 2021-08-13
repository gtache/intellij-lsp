package com.github.gtache.lsp.requests

import com.github.gtache.lsp.PluginMain
import com.github.gtache.lsp.client.languageserver.ServerStatus
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapper
import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
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
        FileDocumentManager.getInstance().getFile(doc)?.let {
            val uri = FileUtils.VFSToURI(it)
            if (uri != null) {
                EditorEventManager.forUri(uri)?.willSave()
            }
        }
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
        val uri = FileUtils.VFSToURI(file)
        if (uri != null) {
            val manager = EditorEventManager.forUri(uri)
            if (manager != null) {
                manager.documentSaved()
                notifyServers(uri, FileChangeType.Changed, manager.wrapper)
            } else {
                notifyServers(uri, FileChangeType.Changed)
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
        val uri = FileUtils.VFSToURI(file)
        if (uri != null) {
            notifyServers(uri, FileChangeType.Deleted)
        }
    }

    private fun notifyServers(uri: String, typ: FileChangeType, wrapper: LanguageServerWrapper? = null): Unit {
        ApplicationUtils.pool {
            val wrappers = PluginMain.getAllServerWrappers()
            wrappers.forEach { w ->
                if (w != wrapper && w.requestManager != null && w.status == ServerStatus.STARTED)
                    w.didChangeWatchedFiles(uri, typ)
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
        val uri = FileUtils.VFSToURI(file)
        if (uri != null) {
            notifyServers(uri, FileChangeType.Created)
        }
    }

}