package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.utils.{FileUtils, Utils}
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentSynchronizationVetoer

class LSPFileDocumentSynchronizationVetoer extends FileDocumentSynchronizationVetoer {
  override def maySaveDocument(document: Document, isSaveExplicit: Boolean): Boolean = {
    EditorEventManager.forUri(FileUtils.documentToUri(document)) match {
      case Some(m) =>
        if (m.needSave) {
          m.needSave = false
          super.maySaveDocument(document, isSaveExplicit)
        }
        else if (m.wrapper.isWillSaveWaitUntil) false
        else super.maySaveDocument(document, isSaveExplicit)
      case None =>
        super.maySaveDocument(document, isSaveExplicit)
    }
  }
}
