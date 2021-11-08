package com.github.gtache.lsp.requests.services.project

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile

/**
 * Service handling reformat actions
 */
interface ReformatProjectService {
    /**
     * Reformats all the files in the project
     */
    fun reformatAllFiles(): Boolean

    /**
     * Reformats the given [file]
     */
    fun reformatFile(file: VirtualFile): Unit

    /**
     * Reformats the given [editor]
     */
    fun reformatFile(editor: Editor): Unit

    /**
     * Reformats the selection in the given [editor]
     */
    fun reformatSelection(editor: Editor): Unit
}