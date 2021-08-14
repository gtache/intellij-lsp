package com.github.gtache.lsp.requests

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.headOrNull
import com.github.gtache.lsp.utils.ApplicationUtils.invokeLater
import com.github.gtache.lsp.utils.ApplicationUtils.writeAction
import com.github.gtache.lsp.utils.DocumentUtils
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.usageView.UsageInfo
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit
import java.net.URL

/**
 * An Object handling WorkspaceEdits
 */
object WorkspaceEditHandler {

    private val logger: Logger = Logger.getInstance(WorkspaceEditHandler::class.java)

    fun applyEdit(
        elem: PsiElement,
        newName: String,
        infos: Array<UsageInfo>,
        listener: RefactoringElementListener?,
        openedEditors: Iterable<VirtualFile>
    ): Unit {
        val edits = HashMap<String, MutableList<TextEdit>>()
        when (elem) {
            is LSPPsiElement -> {
                if (infos.all { info -> info.element is LSPPsiElement }) {
                    infos.forEach { ui ->
                        ui.virtualFile?.let { vf ->
                            FileUtils.editorFromVirtualFile(vf, ui.project)?.let { editor ->
                                ui.element?.textRange?.let { range ->
                                    val lspRange = Range(
                                        DocumentUtils.offsetToLSPPos(editor, range.startOffset),
                                        DocumentUtils.offsetToLSPPos(editor, range.endOffset)
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
                    edits.forEach { edit ->
                        mapping[edit.key] = edit.value
                    }
                    val workspaceEdit = WorkspaceEdit(mapping)
                    applyEdit(workspaceEdit, "Rename " + elem.name + " to " + newName, openedEditors)
                }
            }
            else -> logger.warn("Not an LSPPsiElement : $elem")
        }
    }

    /**
     * Applies a WorkspaceEdit
     *
     * @param edit The edit
     * @return True if everything was applied, false otherwise
     */
    fun applyEdit(edit: WorkspaceEdit, name: String = "LSP edits", toClose: Iterable<VirtualFile> = ArrayList()): Boolean {
        val dChanges = if (edit.documentChanges != null) edit.documentChanges else null
        var didApply = true

        invokeLater {
            var curProject: Project? = null
            val openedEditors = ArrayList<VirtualFile>()

            /**
             * Opens an editor when needed and gets the Runnable
             *
             * @param edits   The text edits
             * @param uri     The uri of the file
             * @param version The version of the file
             * @return The runnable containing the edits
             */
            fun manageUnopenedEditor(edits: Iterable<TextEdit>, uri: String, version: Int = Int.MAX_VALUE): Runnable? {
                val projects = ProjectManager.getInstance().openProjects
                val project = projects //Infer the project from the uri
                    .filter { p -> !p.isDefault }
                    .mapNotNull { p ->
                        p.guessProjectDir()?.let { vf ->
                            FileUtils.VFSToURI(vf)?.let {
                                Pair(it, p)
                            }
                        }
                    }
                    .filter { p -> uri.startsWith(p.first) }
                    .sortedBy { s -> s.first.length }.reversed()
                    .map { p -> p.second }
                    .headOrNull ?: projects[0]
                return FileUtils.openClosedEditor(uri, project)?.let {
                    openedEditors += it.first
                    curProject = it.second.project
                    EditorEventManager.forEditor(it.second)?.getEditsRunnable(version, edits, name)
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
                        val manager = EditorEventManager.forUri(uri)
                        val runnable = if (manager != null) {
                            curProject = manager.editor.project
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
                val manager = EditorEventManager.forUri(uri)
                val runnable = if (manager != null) {
                    curProject = manager.editor.project
                    manager.getEditsRunnable(edits = changes, name = name)
                } else manageUnopenedEditor(changes, uri)
                toApply += runnable
            }
            if (toApply.contains(null)) {
                logger.warn("Didn't apply, null runnable")
                didApply = false
            } else {
                val project = curProject!!
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