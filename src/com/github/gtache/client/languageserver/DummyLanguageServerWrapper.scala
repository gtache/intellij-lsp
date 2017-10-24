package com.github.gtache.client.languageserver

import com.github.gtache.client.RequestManager
import com.github.gtache.editor.EditorEventManager
import com.intellij.openapi.editor.Editor
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.services.LanguageServer


/**
  * Class representing an unusable LanguageServerWrapper, indicating that a working one is being instantiated
  */
class DummyLanguageServerWrapper extends LanguageServerWrapper {
  override def connect(editor: Editor): Unit = throw new UnsupportedOperationException

  override def disconnect(path: String): Unit = throw new UnsupportedOperationException

  override def getEditorManagerFor(uri: String): EditorEventManager = throw new UnsupportedOperationException

  override def getLanguageId(contentTypes: Array[String]): String = throw new UnsupportedOperationException

  override def getRequestManager: RequestManager = throw new UnsupportedOperationException

  override def start(): Unit = throw new UnsupportedOperationException

  override def isActive: Boolean = throw new UnsupportedOperationException

  override def isConnectedTo(location: String): Boolean = throw new UnsupportedOperationException

  override def getServer: LanguageServer = throw new UnsupportedOperationException

  override def getServerCapabilities: ServerCapabilities = throw new UnsupportedOperationException

  override def logMessage(message: Message): Unit = throw new UnsupportedOperationException

  override def stop(): Unit = throw new UnsupportedOperationException
}
