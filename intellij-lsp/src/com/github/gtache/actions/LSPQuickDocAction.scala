package com.github.gtache.actions

import com.github.gtache.editor.EditorEventManager
import com.intellij.codeInsight.documentation.actions.ShowQuickDocInfoAction
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware

/**
  * Action overriding QuickDoc (CTRL+Q) : If editor / document is LSP-supported, shows the LSP doc, otherwise shows the default doc
  */
class LSPQuickDocAction extends ShowQuickDocInfoAction with DumbAware {
  private val LOG: Logger = Logger.getInstance(classOf[LSPQuickDocAction])

  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    EditorEventManager.forEditor(editor) match {
      case Some(manager) => manager.quickDoc(editor)
      case None => super.actionPerformed(e)
    }
  }
}
