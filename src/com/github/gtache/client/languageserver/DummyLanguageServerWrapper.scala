package com.github.gtache.client.languageserver

import com.github.gtache.client.RequestManager
import com.github.gtache.editor.EditorEventManager
import com.intellij.openapi.editor.Editor
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.services.LanguageServer

class DummyLanguageServerWrapper extends LanguageServerWrapper {
  override def connect(editor: Editor): Unit = throw new UnsupportedOperationException(editor.getDocument.getText())

  override def disconnect(path: String): Unit = throw new UnsupportedOperationException

  override def getEditorManagerFor(uri: String): EditorEventManager = throw new UnsupportedOperationException

  override def getLanguageId(contentTypes: Array[String]): String = throw new UnsupportedOperationException

  override def getRequestManager: RequestManager = throw new UnsupportedOperationException

  /**
    * Starts the LanguageServer
    */
  override def start(rootFolder: String): Unit = throw new UnsupportedOperationException

  /**
    * @return whether the underlying connection to language languageServer is still active
    */
  override def isActive: Boolean = throw new UnsupportedOperationException

  /**
    * Checks if the wrapper is already connected to the document at the given path
    */
  override def isConnectedTo(location: String): Boolean = throw new UnsupportedOperationException

  /**
    * @return the LanguageServer
    */
  override def getServer: LanguageServer = throw new UnsupportedOperationException

  /**
    * @return the languageServer capabilities, or null if initialization job didn't complete
    */
  override def getServerCapabilities: ServerCapabilities = throw new UnsupportedOperationException

  override def logMessage(message: Message): Unit = throw new UnsupportedOperationException

  override def stop(): Unit = throw new UnsupportedOperationException
}
