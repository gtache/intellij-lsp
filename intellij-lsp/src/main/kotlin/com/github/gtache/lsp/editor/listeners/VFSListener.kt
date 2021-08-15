package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.requests.FileEventManager
import com.intellij.openapi.vfs.*

/**
 * Object listening to file system changes
 */
object VFSListener : VirtualFileListener {
    /**
     * Fired when a virtual file is renamed from ,in IDEA, or its writable status is changed.
     * For files renamed externally, {@link #fileCreated} and {@link #fileDeleted} events will be fired.
     *
     * @param event the event object containing information about the change.
     */
    override fun propertyChanged(event: VirtualFilePropertyEvent) {
        if (event.propertyName == VirtualFile.PROP_NAME) FileEventManager.fileRenamed(event.oldValue as String, event.newValue as String)
    }

    /**
     * Fired when the contents of a virtual file is changed.
     *
     * @param event the event object containing information about the change.
     */
    override fun contentsChanged(event: VirtualFileEvent): Unit {
        FileEventManager.fileChanged(event.file)
    }

    /**
     * Fired when a virtual file is deleted.
     *
     * @param event the event object containing information about the change.
     */
    override fun fileDeleted(event: VirtualFileEvent): Unit {
        FileEventManager.fileDeleted(event.file)
    }

    /**
     * Fired when a virtual file is moved from ,in IDEA.
     *
     * @param event the event object containing information about the change.
     */
    override fun fileMoved(event: VirtualFileMoveEvent): Unit {
        FileEventManager.fileMoved(event.file)
    }

    /**
     * Fired when a virtual file is copied from ,in IDEA.
     *
     * @param event the event object containing information about the change.
     */
    override fun fileCopied(event: VirtualFileCopyEvent): Unit {
        fileCreated(event)
    }

    /**
     * Fired when a virtual file is created. This event is not fired for files discovered during initial VFS initialization.
     *
     * @param event the event object containing information about the change.
     */
    override fun fileCreated(event: VirtualFileEvent): Unit {
        FileEventManager.fileCreated(event.file)
    }

    /**
     * Fired before the change of a name or writable status of a file is processed.
     *
     * @param event the event object containing information about the change.
     */
    override fun beforePropertyChange(event: VirtualFilePropertyEvent): Unit {
    }

    /**
     * Fired before the change of contents of a file is processed.
     *
     * @param event the event object containing information about the change.
     */
    override fun beforeContentsChange(event: VirtualFileEvent): Unit {
    }

    /**
     * Fired before the deletion of a file is processed.
     *
     * @param event the event object containing information about the change.
     */
    override fun beforeFileDeletion(event: VirtualFileEvent): Unit {
    }

    /**
     * Fired before the movement of a file is processed.
     *
     * @param event the event object containing information about the change.
     */
    override fun beforeFileMovement(event: VirtualFileMoveEvent): Unit {
    }
}