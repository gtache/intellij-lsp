package com.github.gtache.lsp.requests.services.project

import com.github.gtache.lsp.editor.services.application.EditorApplicationService
import com.github.gtache.lsp.editor.services.project.EditorProjectService
import com.github.gtache.lsp.services.project.LSPProjectService
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
 * Implementation of ReformatHandlerService
 */
class ReformatProjectServiceImpl(private val project: Project) : ReformatProjectService {

    override fun reformatAllFiles(): Boolean {
        var allFilesSupported = true
        ProjectFileIndex.getInstance(project).iterateContent { fileOrDir ->
            if (fileOrDir.isDirectory) {
                true
            } else {
                if (project.service<LSPProjectService>().isExtensionSupported(fileOrDir.extension)) {
                    reformatFile(fileOrDir)
                    true
                } else {
                    allFilesSupported = false
                    true
                }
            }
        }
        return allFilesSupported
    }

    override fun reformatFile(file: VirtualFile) {
        if (project.service<LSPProjectService>().isExtensionSupported(file.extension)) {
            val uri = FileUtils.vfsToURI(file)
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
                            service<EditorApplicationService>().managerForEditor(editor)?.reformat(closeAfter = true)
                        }
                    }
                }
            }
        }
    }

    override fun reformatFile(editor: Editor) {
        service<EditorApplicationService>().managerForEditor(editor)?.reformat()
    }

    override fun reformatSelection(editor: Editor) {
        service<EditorApplicationService>().managerForEditor(editor)?.reformatSelection()
    }
}