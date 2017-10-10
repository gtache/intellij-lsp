package com.github.gtache.editor

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}

class DocumentListenerImpl extends DocumentListener {

  private val LOG: Logger = Logger.getInstance(classOf[DocumentListenerImpl])
  private var manager: EditorEventManager = _

  /**
    * Called before the text of the document is changed.
    *
    * @param event the event containing the information about the change.
    */
  override def beforeDocumentChange(event: DocumentEvent): Unit = {
  }


  /**
    * Called after the text of the document has been changed.
    *
    * @param event the event containing the information about the change.
    */
  override def documentChanged(event: DocumentEvent): Unit = {
    if (manager != null) {
      manager.documentChanged(event)
    } else {
      LOG.error("No manager set!")
    }
  }

  def setManager(manager: EditorEventManager): Unit = {
    this.manager = manager
  }
}
