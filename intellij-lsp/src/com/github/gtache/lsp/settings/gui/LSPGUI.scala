package com.github.gtache.lsp.settings.gui

import com.intellij.openapi.options.Configurable

trait LSPGUI {

  def isModified: Boolean

  def reset(): Unit

  def apply(): Unit


}
