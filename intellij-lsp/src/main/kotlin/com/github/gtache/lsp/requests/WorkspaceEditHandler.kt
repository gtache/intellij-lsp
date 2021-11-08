package com.github.gtache.lsp.requests

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.services.application.EditorApplicationService
import com.github.gtache.lsp.editor.services.project.EditorProjectService
import com.github.gtache.lsp.utils.ApplicationUtils.invokeLater
import com.github.gtache.lsp.utils.ApplicationUtils.writeAction
import com.github.gtache.lsp.utils.DocumentUtils
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.usageView.UsageInfo
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit
import java.net.URL

/**
 * An object handling WorkspaceEdits
 */
object WorkspaceEditHandler {

    private val logger: Logger = Logger.getInstance(WorkspaceEditHandler::class.java)

    /**
     * Applies the given edit specified by the [element] to edit, the [newName] to give to this [element], various [infos] regarding the usage, a [listener] to notify and the list of [openedEditors]
     */
    fun applyEdit(
        element: PsiElement,
        newName: String,
        infos: Array<UsageInfo>,
        listener: RefactoringElementListener?,
        openedEditors: Iterable<VirtualFile>
    ): Unit {
        val edits = HashMap<String, MutableList<TextEdit>>()
        when (element) {
            is LSPPsiElement -> {
                if (infos.all { info -> info.element is LSPPsiElement }) {
                    infos.forEach { ui ->
                        ui.virtualFile?.let { vf ->
                            FileUtils.editorFromVirtualFile(vf, ui.project)?.let { editor ->
                                ui.element?.textRange?.let { range ->
                                    val lspRange = Range(
                                        DocumentUtils.offsetToLSPPosition(editor, range.startOffset),
                                        DocumentUtils.offsetToLSPPosition(editor, range.endOffset)
                                    )
                                    val edit = TextEdit(lspRange, newName)
                                    val uri = FileUtils.sanitizeURI(URL(vf.url.replace(" ", FileUtils.SPACE_ENCODED)).toURI().toString())
                                    if (edits.contains(uri)) {
                                        edits[uri]!! += edit
                                    } else {
                                        edits[uri] = mutableListOf(edit)
                                    }
                                }
                            }
                        }
                    }
                    val mapping = HashMap<String, List<TextEdit>>()
                    edits.forEach { (key, value) ->
                        mapping[key] = value
                    }
                    val workspaceEdit = WorkspaceEdit(mapping)
                    applyEdit(workspaceEdit, element.project, "Rename " + element.name + " to " + newName, openedEditors)
                }
            }
            else -> logger.warn("Not an LSPPsiElement : $element")
        }
    }

    /**
     * Applies an [edit] named as [name] to the given [project] and closes the files [toClose]
     *
     * @param edit The edit
     * Returns True if everything was applied, false otherwise
     */
    fun applyEdit(edit: WorkspaceEdit, project: Project, name: String = "LSP edits", toClose: Iterable<VirtualFile> = ArrayList()): Boolean {
        val dChanges = if (edit.documentChanges != null) edit.documentChanges else null
        var didApply = true

        invokeLater {
            val openedEditors = ArrayList<VirtualFile>()

            /**
             * Opens if required an editor specified by the given file [uri], and returns a Runnable applying the given [edits] if [version] is greater than the editor version
             *
             * @param edits   The text edits
             * @param uri     The uri of the file
             * @param version The version of the file
             * Returns The runnable containing the edits
             */
            fun manageUnopenedEditor(edits: Iterable<TextEdit>, uri: String, version: Int = Int.MAX_VALUE): Runnable? {
                return FileUtils.openClosedEditor(uri, project)?.let {
                    openedEditors += it.first
                    service<EditorApplicationService>().managerForEditor(it.second)?.getEditsRunnable(version, edits, name)
                }
            }

            //Get the runnable of edits for each editor to apply them all in one command
            val toApply = ArrayList<Runnable?>()
            if (dChanges != null) {
                dChanges.forEach { edit ->
                    if (edit.isLeft) {
                        val textEdit = edit.left
                        val doc = textEdit.textDocument
                        val version = doc.version
                        val uri = FileUtils.sanitizeURI(doc.uri)
                        val manager = project.service<EditorProjectService>().forUri(uri)
                        val runnable = if (manager != null) {
                            manager.getEditsRunnable(version, textEdit.edits, name)
                        } else manageUnopenedEditor(textEdit.edits, uri, version)
                        toApply += runnable
                    } else if (edit.isRight) {
                        val resourceOp = edit.right
                        //TODO
                    } else {
                        logger.warn("Null edit")
                    }

                }
            } else (if (edit.changes != null) edit.changes else null)?.forEach { edit ->
                val uri = FileUtils.sanitizeURI(edit.key)
                val changes = edit.value
                val manager = project.service<EditorProjectService>().forUri(uri)
                val runnable = if (manager != null) {
                    manager.getEditsRunnable(edits = changes, name = name)
                } else manageUnopenedEditor(changes, uri)
                toApply += runnable
            }
            if (toApply.contains(null)) {
                logger.warn("Didn't apply, null runnable")
                didApply = false
            } else {
                val runnable = Runnable {
                    toApply.filterNotNull().forEach { r -> r.run() }
                }
                invokeLater {
                    writeAction {
                        CommandProcessor.getInstance().executeCommand(project, runnable, name, "LSPPlugin", UndoConfirmationPolicy.DEFAULT, false)
                        (openedEditors + toClose).forEach { f -> FileEditorManager.getInstance(project).closeFile(f) }
                    }
                }
            }
        }
        return didApply
    }
}