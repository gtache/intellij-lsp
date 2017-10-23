package com.github.gtache.editor

import com.github.gtache.PluginMain
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction

class LSPQuickDocAction extends DumbAwareAction {
  private val LOG: Logger = Logger.getInstance(classOf[LSPQuickDocAction])

  override def actionPerformed(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val manager = PluginMain.getManagerForEditor(editor)
    if (manager != null) {
      manager.quickDoc(editor)
    }
  }
}
