package com.github.gtache.lsp.contributors.rename

import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.editor.services.application.EditorApplicationService
import com.github.gtache.lsp.requests.WorkspaceEditHandler
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo

/**
 * PSIElement rename processor for LSP
 */
class LSPRenameProcessor : RenamePsiElementProcessor() {

    companion object {
        private var openedFiles: MutableSet<VirtualFile> = HashSet()

        /**
         * Clears the files list
         */
        fun clearFiles(): Unit {
            openedFiles.clear()
        }

        /**
         * Returns the list of all files
         */
        fun getFiles(): Set<VirtualFile> = openedFiles.toSet()

        /**
         * Adds the given files
         */
        fun addEditors(toAdd: Iterable<VirtualFile>): Unit {
            openedFiles += toAdd
        }
    }

    private var curElem: PsiElement? = null
    private var elements: MutableSet<PsiElement> = HashSet()

    override fun canProcessElement(element: PsiElement): Boolean {
        when (element) {
            is LSPPsiElement -> return true
            is PsiFile -> {
                val editor = FileEditorManager.getInstance(element.project).getAllEditors(element.virtualFile).filterIsInstance<TextEditor>()
                    .map { t -> t.editor }.firstOrNull()
                if (editor != null) {
                    val manager = service<EditorApplicationService>().managerForEditor(editor)
                    if (manager != null) {
                        return if (editor.contentComponent.hasFocus()) {
                            val offset = editor.caretModel.currentCaret.offset
                            val (elements, openedEditors) = manager.references(offset, getOriginalElement = true)
                            this.elements += elements.toSet()
                            LSPRenameProcessor.openedFiles += openedEditors.toSet()
                            this.curElem = elements.find { e ->
                                val range = e.textRange
                                val start = range.startOffset
                                val end = range.endOffset
                                (start <= offset) && (offset <= end)
                            }
                            this.elements = this.elements.filterTo(HashSet()) { elem -> (elem as PsiNamedElement).name == (curElem as PsiNamedElement).name }
                            true
                        } else false
                    } else {
                        return false
                    }
                } else return false
            }
            else -> return false
        }
    }

    override fun createRenameDialog(project: Project, element: PsiElement, nameSuggestionContext: PsiElement?, editor: Editor?): RenameDialog {
        return super.createRenameDialog(project, curElem!!, nameSuggestionContext, editor)
    }

    override fun findReferences(element: PsiElement, searchScope: SearchScope, searchInCommentsAndStrings: Boolean): Collection<PsiReference> {
        return when (element) {
            is LSPPsiElement -> if (elements.contains(element)) {
                elements.mapNotNull { e -> e.reference }
            } else {
                val manager = FileUtils.editorFromPsiFile(element.containingFile)?.let { service<EditorApplicationService>().managerForEditor(it) }
                if (manager != null) {
                    val refs = manager.references(element.textOffset, getOriginalElement = true)
                    openedFiles += refs.second
                    refs.first.mapNotNull { p -> p.reference }.toList()
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    override fun isInplaceRenameSupported(): Boolean = true

    //TODO may rename invalid elements
    override fun renameElement(element: PsiElement, newName: String, usages: Array<UsageInfo>, listener: RefactoringElementListener?) {
        when (element) {
            is LSPPsiElement -> service<EditorApplicationService>().managerForEditor(element.editor)?.let { m ->
                m.rename(newName, m.editor.caretModel.currentCaret.offset - 1)
            }
            else -> WorkspaceEditHandler.applyEdit(element, newName, usages, listener, openedFiles.toList())
        }
        openedFiles.clear()
        elements.clear()
        curElem = null
    }
}