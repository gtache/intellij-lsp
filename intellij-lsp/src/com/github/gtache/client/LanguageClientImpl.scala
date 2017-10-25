package com.github.gtache.client

import java.util.concurrent.CompletableFuture

import com.github.gtache.PluginMain
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
      val edit = params.getEdit
      import scala.collection.JavaConverters._
      val changes = edit.getChanges.asScala
      val dChanges = edit.getDocumentChanges.asScala
      var didApply: Boolean = true
      if (dChanges != null) {
        dChanges.foreach(edit => {
          val doc = edit.getTextDocument
          val version = doc.getVersion
          val uri = doc.getUri
          val manager = PluginMain.getManagerForURI(uri)
          if (manager != null) {
            if (!manager.applyEdit(version, edit.getEdits.asScala.toList)) didApply = false
          }
        })
      } else {
        changes.foreach(edit => {
          val uri = edit._1
          val changes = edit._2.asScala
          val manager = PluginMain.getManagerForURI(uri)
          if (manager != null) {
            if (!manager.applyEdit(edits = changes.toList)) didApply = false
          }
        })
      }
      new ApplyWorkspaceEditResponse(didApply)
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
    for (diagnostic <- diagnostics.asScala) {
      val code = diagnostic.getCode
      val message = diagnostic.getMessage
      val source = diagnostic.getSource
      val range = diagnostic.getRange
      val severity = diagnostic.getSeverity
    }
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
