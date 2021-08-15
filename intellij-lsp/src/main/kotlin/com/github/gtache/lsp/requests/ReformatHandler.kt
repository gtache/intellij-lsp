package com.github.gtache.lsp.requests

import com.github.gtache.lsp.LSPProjectService
import com.github.gtache.lsp.editor.EditorApplicationService
import com.github.gtache.lsp.editor.EditorProjectService
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile

/**
 * Object handling reformat events
 */
object ReformatHandler {

    /**
     * Unused
     * Reformats all the files in the project
     *
     * @param project The project
     * @return True if all the files were supported by the language servers, false otherwise
     */
    fun reformatAllFiles(project: Project): Boolean {
        var allFilesSupported = true
        ProjectFileIndex.getInstance(project).iterateContent { fileOrDir ->
            if (fileOrDir.isDirectory) {
                true
            } else {
                if (project.service<LSPProjectService>().isExtensionSupported(fileOrDir.extension)) {
                    reformatFile(fileOrDir, project)
                    true
                } else {
                    allFilesSupported = false
                    true
                }
            }
        }
        return allFilesSupported
    }

    /**
     * Reformat a file given a VirtualFile and a Project
     *
     * @param file    The file
     * @param project The project
     */
    fun reformatFile(file: VirtualFile, project: Project): Unit {
        if (project.service<LSPProjectService>().isExtensionSupported(file.extension)) {
            val uri = FileUtils.VFSToURI(file)
            if (uri != null) {
                val manager = project.service<EditorProjectService>().forUri(uri)
                if (manager != null) {
                    manager.reformat()
                } else {
                    ApplicationUtils.invokeLater {
                        val fileEditorManager = FileEditorManager.getInstance(project)
                        val descriptor = OpenFileDescriptor(project, file)
                        val editor = ApplicationUtils.computableWriteAction {
                            fileEditorManager.openTextEditor(descriptor, false)
                        }
                        if (editor != null) {
                            service<EditorApplicationService>().forEditor(editor)?.reformat(closeAfter = true)
                        }
                    }
                }
            }
        }
    }

    /**
     * Reformat a file given its editor
     *
     * @param editor The editor
     */
    fun reformatFile(editor: Editor): Unit {
        service<EditorApplicationService>().forEditor(editor)?.reformat()
    }


    /**
     * Reformat a selection in a file given its editor
     *
     * @param editor The editor
     */
    fun reformatSelection(editor: Editor): Unit {
        service<EditorApplicationService>().forEditor(editor)?.reformatSelection()
    }

}