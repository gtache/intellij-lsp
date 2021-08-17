package com.github.gtache.lsp.actions

import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapperImpl
import com.github.gtache.lsp.services.project.LSPProjectService
import com.github.gtache.lsp.settings.gui.ComboCheckboxDialog
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class LinkToServerAction : DumbAwareAction() {
    companion object {
        private val logger: Logger = Logger.getInstance(LinkToServerAction::class.java)
    }

    override fun actionPerformed(anActionEvent: AnActionEvent): Unit {
        var editors: List<Editor?> = emptyList()
        val project = anActionEvent.project
        if (project != null) {
            when (anActionEvent.place) {
                ActionPlaces.EDITOR_POPUP -> {
                    editors = listOf(anActionEvent.dataContext.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE))
                }
                ActionPlaces.EDITOR_TAB_POPUP -> {
                    val virtualFile = anActionEvent.dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
                    if (virtualFile != null) {
                        editors = listOf(FileUtils.editorFromVirtualFile(virtualFile, project))
                        if (editors[0] == null) {
                            val psiFile = anActionEvent.dataContext.getData(CommonDataKeys.PSI_FILE)
                            if (psiFile != null) {
                                editors = listOf(FileUtils.editorFromPsiFile(psiFile))
                            }
                        }
                    }
                }
                ActionPlaces.PROJECT_VIEW_POPUP -> {
                    val virtualFiles = anActionEvent.dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
                    if (virtualFiles != null) {
                        editors = openClosedEditors(virtualFiles.mapNotNull { v -> FileUtils.VFSToURI(v) }, project)
                    }
                }
                else -> {
                    editors = listOf(anActionEvent.dataContext.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE))
                    logger.warn("Unknown place : " + anActionEvent.place)
                }
            }
            val allEditors = editors.filterNotNull().partition { e -> LanguageServerWrapperImpl.forEditor(e) == null }
            val tmpEditors = allEditors.first.toMutableList()
            tmpEditors.addAll(allEditors.second)
            if (!tmpEditors.all { e -> e.project == project }) {
                Messages.showWarningDialog(
                    project,
                    "You're trying to connect editors that are not part of the current project. This is not possible.",
                    "Trying to force connect editors outside project"
                )
            } else {
                editors = tmpEditors
                val alreadyConnected = allEditors.second
                if (alreadyConnected.isNotEmpty()) {
                    Messages.showWarningDialog(
                        project,
                        "Editor(s) " + alreadyConnected.joinToString("\n") { e ->
                            FileDocumentManager.getInstance().getFile(e.document)?.name ?: "null"
                        } + " already connected to servers , ext " + alreadyConnected.joinToString(",") { e ->
                            LanguageServerWrapperImpl.forEditor(e)?.serverDefinition?.ext ?: "null"
                        } + ", will be overwritten",
                        "Trying to connect an already connected editor"
                    )
                }
                if (editors.isNotEmpty()) {
                    val projectService = project.service<LSPProjectService>()
                    val allDefinitions = projectService.extToServerDefinition.values.distinct()
                    val allDefinitionNames = allDefinitions.map { d -> d.ext }
                    val allWrappers = projectService.getAllWrappers()
                    val allWrapperNames = allWrappers.map { w -> w.serverDefinition.ext + " : " + w.project.name }
                    val dialog = ComboCheckboxDialog(
                        project,
                        "Link a file to a language server",
                        allDefinitionNames,
                        allWrapperNames
                    )
                    dialog.show()
                    val exitCode = dialog.exitCode
                    if (exitCode >= 0) {
                        editors.forEach { editor ->
                            val fileType = FileUtils.fileTypeFromEditor(editor)
                            if (projectService.isExtensionSupported(fileType?.defaultExtension)) {
                                val ret = Messages.showOkCancelDialog(
                                    editor.project,
                                    "This file extension" + fileType?.defaultExtension + " is already supported by a Language Server, continue?",
                                    "Known extension", "Ok", "Cancel", Messages.getWarningIcon()
                                )
                                if (ret == Messages.OK) {
                                    projectService.forceEditorOpened(editor, allDefinitions[exitCode], project)
                                }
                            } else projectService.forceEditorOpened(editor, allDefinitions[exitCode], project)
                        }
                    }
                }
            }
        }
    }

    private fun openClosedEditors(uris: Iterable<String>, project: Project): List<Editor> {
        return uris.mapNotNull { uri ->
            var editor = FileUtils.editorFromUri(uri, project)
            if (editor == null) {
                editor = FileUtils.openClosedEditor(uri, project)?.second
            }
            editor
        }
    }
}