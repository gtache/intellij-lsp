package com.github.gtache.lsp.settings.gui

trait LSPGUI {

  def isModified: Boolean

  def reset(): Unit

  def apply(): Unit


}
