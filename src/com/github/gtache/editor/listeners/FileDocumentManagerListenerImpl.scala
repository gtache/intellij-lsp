package com.github.gtache.editor.listeners

import com.github.gtache.PluginMain
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.vfs.VirtualFile

object FileDocumentManagerListenerImpl extends FileDocumentManagerListener {
  override def beforeDocumentSaving(document: Document): Unit = PluginMain.willSave(document)

  override def unsavedDocumentsDropped(): Unit = {}

  override def beforeAllDocumentsSaving(): Unit = PluginMain.willSaveAllDocuments()

  override def beforeFileContentReload(virtualFile: VirtualFile, document: Document): Unit = {}

  override def fileWithNoDocumentChanged(virtualFile: VirtualFile): Unit = {}

  override def fileContentReloaded(virtualFile: VirtualFile, document: Document): Unit = {}

  override def fileContentLoaded(virtualFile: VirtualFile, document: Document): Unit = {}
}
