package com.github.gtache.client

import java.util.concurrent.CompletableFuture

import com.github.gtache.editor.EditorEventManager
import com.github.gtache.requests.WorkspaceEditHandler
import com.intellij.openapi.diagnostic.Logger
import org.eclipse.lsp4j._
import org.eclipse.lsp4j.services.{LanguageClient, LanguageServer}

import scala.collection.JavaConverters._


/**
  * Implementation of the LanguageClient
  */
class LanguageClientImpl extends LanguageClient {
  private val LOG: Logger = Logger.getInstance(classOf[LanguageClientImpl])
  private var server: LanguageServer = _

  /**
    * Connects the LanguageClient to the server
    *
    * @param server The LanguageServer
    */
  def connect(server: LanguageServer): Unit = {
    this.server = server
  }

  override def applyEdit(params: ApplyWorkspaceEditParams): CompletableFuture[ApplyWorkspaceEditResponse] = {
    CompletableFuture.supplyAsync(() => {
      new ApplyWorkspaceEditResponse(WorkspaceEditHandler.applyEdit(params.getEdit))
    })
  }

  override def registerCapability(params: RegistrationParams): CompletableFuture[Void] = null

  override def unregisterCapability(params: UnregistrationParams): CompletableFuture[Void] = null

  override def telemetryEvent(o: Any): Unit = {
    //TODO
  }

  override def publishDiagnostics(publishDiagnosticsParams: PublishDiagnosticsParams): Unit = {
    val uri = publishDiagnosticsParams.getUri
    val diagnostics = publishDiagnosticsParams.getDiagnostics
    EditorEventManager.forUri(uri).foreach(e => e.diagnostics(diagnostics.asScala))
  }

  override def showMessage(messageParams: MessageParams): Unit = {
    MessageDialog.main(messageParams.getMessage)
  }

  override def showMessageRequest(showMessageRequestParams: ShowMessageRequestParams): CompletableFuture[MessageActionItem] = {
    val actions = showMessageRequestParams.getActions
    for (action <- actions.asScala) {
    }
    MessageDialog.main(showMessageRequestParams.getMessage)
    CompletableFuture.completedFuture(new MessageActionItem())
  }

  override def logMessage(messageParams: MessageParams): Unit = {
    val message = messageParams.getMessage
    messageParams.getType match {
      case MessageType.Error =>
        LOG.error(message)
      case MessageType.Warning =>
        LOG.warn(message)
      case MessageType.Info =>
        LOG.info(message)
      case MessageType.Log =>
        LOG.debug(message)
      case _ =>
        throw new IllegalArgumentException("Unknown message type")
    }
  }
}
