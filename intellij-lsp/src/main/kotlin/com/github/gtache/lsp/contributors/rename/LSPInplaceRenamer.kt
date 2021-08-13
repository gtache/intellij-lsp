package com.github.gtache.lsp.contributors.rename

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.inplace.MemberInplaceRenamer

class LSPInplaceRenamer(
    elementToRename: PsiNamedElement,
    substituted: PsiElement,
    val editor: Editor,
    initialName: String? = elementToRename.name,
    oldName: String? = elementToRename.name
) : MemberInplaceRenamer(elementToRename, substituted, editor, initialName, oldName) {

    companion object {
        private val logger: Logger = Logger.getInstance(LSPInplaceRenamer::class.java)
    }

    override fun collectRefs(referencesSearchScope: SearchScope): Collection<PsiReference> {
        val manager = EditorEventManager.forEditor(editor)
        return if (manager != null) {
            val (references, toClose) = manager.references(editor.caretModel.currentCaret.offset, getOriginalElement = true)
            LSPRenameProcessor.addEditors(toClose)
            references.mapNotNull { f -> f.reference }.toList()
        } else {
            emptyList()
        }
    }
}