package com.github.gtache.lsp.settings.gui

import javax.swing.JPanel

import com.github.gtache.lsp.settings.LSPState

trait LSPGUI {

  import LSPGUI.lspState

  def state: LSPState = lspState

  def isModified: Boolean

  def reset(): Unit

  def apply(): Unit

  def getRootPanel: JPanel

}

object LSPGUI {
  val lspState: LSPState = LSPState.getInstance()
}
