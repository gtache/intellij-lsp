package com.github.gtache.lsp.editor.listeners

import com.github.gtache.lsp.editor.EditorEventManager

abstract class AbstractLSPListener : LSPListener {
    override var manager: EditorEventManager? = null
    override var enabled: Boolean = true
}