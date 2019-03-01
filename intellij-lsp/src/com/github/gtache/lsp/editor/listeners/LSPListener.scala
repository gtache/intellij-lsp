package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.editor.EditorEventManager
import com.intellij.openapi.diagnostic.Logger

/**
  * Interface for all the LSP listeners depending on a manager
  */
trait LSPListener {
  private val LOG: Logger = Logger.getInstance(this.getClass)
  protected var manager: EditorEventManager = _
  protected var enabled: Boolean = true

  /**
    * Sets the manager for this listener
    *
    * @param manager The manager
    */
  def setManager(manager: EditorEventManager): Unit = {
    this.manager = manager
  }

  /**
    * Checks if the listener must currently report on events
    */
  protected def checkEnabled(): Boolean = {
    if (manager == null) {
      LOG.error("Manager is null")
      false
    } else enabled
  }

  def disable(): Unit = {
    enabled = false
  }

  def enable(): Unit = {
    enabled = true
  }

}
