/* Adapted from lsp4e */
package com.github.gtache.client.languageserver

import java.io.{File, IOException}
import java.net.URI
import java.util.concurrent._

import com.github.gtache.client._
import com.github.gtache.client.connection.StreamConnectionProvider
import com.github.gtache.editor.EditorEventManager
import com.github.gtache.editor.listeners.{DocumentListenerImpl, EditorMouseListenerImpl, EditorMouseMotionListenerImpl, SelectionListenerImpl}
import com.github.gtache.requests.Timeout
import com.github.gtache.{PluginMain, Utils}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.{Either, Message, ResponseErrorCode, ResponseMessage}
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer
import org.jetbrains.annotations.Nullable

import scala.collection.mutable

object LanguageServerWrapperImpl {
  private val uriToLanguageServerWrapper: mutable.Map[String, LanguageServerWrapper] = mutable.HashMap()
  private val editorToLanguageServerWrapper: mutable.Map[Editor, LanguageServerWrapper] = mutable.HashMap()

  def forUri(uri: String): Option[LanguageServerWrapper] = {
    uriToLanguageServerWrapper.get(uri)
  }

  def forEditor(editor: Editor): Option[LanguageServerWrapper] = {
    editorToLanguageServerWrapper.get(editor)
  }
}

/**
  * The working implementation of a LanguageServerWrapper
  *
  * @param serverDefinition The serverDefinition
  * @param rootPath       The root directory
  */
class LanguageServerWrapperImpl(val serverDefinition: LanguageServerDefinition, val rootPath: String) extends LanguageServerWrapper {

  private val lspStreamProvider: StreamConnectionProvider = serverDefinition.createConnectionProvider(rootPath)
  private val connectedEditors: mutable.Map[String, EditorEventManager] = mutable.HashMap()
  private val LOG: Logger = Logger.getInstance(classOf[LanguageServerWrapperImpl])
  private var languageServer: LanguageServer = _
  private var client: LanguageClientImpl = _
  private var requestManager: RequestManager = _
  private var initializeResult: InitializeResult = _
  private var launcherFuture: Future[_] = _
  private var initializeFuture: CompletableFuture[InitializeResult] = _
  private var capabilitiesAlreadyRequested = false
  private var initializeStartTime = 0L
  private var started: Boolean = false

  import com.github.gtache.client.languageserver.LanguageServerWrapperImpl._

  /**
    * Returns the EditorEventManager for a given uri
    *
    * @param uri the URI as a string
    * @return the EditorEventManager (or null)
    */
  def getEditorManagerFor(uri: String): EditorEventManager = {
    connectedEditors.get(uri).orNull
  }

  /**
    * @return The request manager for this wrapper
    */
  def getRequestManager: RequestManager = {
    requestManager
  }

  /**
    * Starts the LanguageServer
    */
  @throws[IOException]
  def start(): Unit = {
    if (!started) {
      try {
        this.lspStreamProvider.start()
        client = serverDefinition.createLanguageClient
        val initParams = new InitializeParams
        initParams.setRootUri(Utils.pathToUri(rootPath))
        val launcher = LSPLauncher.createClientLauncher(client, this.lspStreamProvider.getInputStream, this.lspStreamProvider.getOutputStream)

        this.languageServer = launcher.getRemoteProxy
        client.connect(languageServer)
        requestManager = new SimpleRequestManager(languageServer, client)
        this.launcherFuture = launcher.startListening
        //TODO update capabilities when implemented
        val workspaceClientCapabilites = new WorkspaceClientCapabilities
        workspaceClientCapabilites.setApplyEdit(true)
        workspaceClientCapabilites.setExecuteCommand(new ExecuteCommandCapabilities)
        workspaceClientCapabilites.setSymbol(new SymbolCapabilities)
        val textDocumentClientCapabilities = new TextDocumentClientCapabilities
        textDocumentClientCapabilities.setCodeAction(new CodeActionCapabilities)
        textDocumentClientCapabilities.setCodeLens(new CodeLensCapabilities)
        textDocumentClientCapabilities.setCompletion(new CompletionCapabilities(new CompletionItemCapabilities(false)))
        textDocumentClientCapabilities.setDefinition(new DefinitionCapabilities)
        textDocumentClientCapabilities.setDocumentHighlight(new DocumentHighlightCapabilities)
        textDocumentClientCapabilities.setDocumentLink(new DocumentLinkCapabilities)
        textDocumentClientCapabilities.setDocumentSymbol(new DocumentSymbolCapabilities)
        textDocumentClientCapabilities.setFormatting(new FormattingCapabilities)
        textDocumentClientCapabilities.setHover(new HoverCapabilities)
        textDocumentClientCapabilities.setOnTypeFormatting(null)

        textDocumentClientCapabilities.setRangeFormatting(new RangeFormattingCapabilities)
        textDocumentClientCapabilities.setReferences(new ReferencesCapabilities)
        textDocumentClientCapabilities.setRename(new RenameCapabilities)
        textDocumentClientCapabilities.setSignatureHelp(new SignatureHelpCapabilities)
        textDocumentClientCapabilities.setSynchronization(new SynchronizationCapabilities(true, false, true))
        initParams.setCapabilities(new ClientCapabilities(workspaceClientCapabilites, textDocumentClientCapabilities, null))
        initParams.setInitializationOptions(this.lspStreamProvider.getInitializationOptions(URI.create(initParams.getRootUri)))
        initializeFuture = languageServer.initialize(initParams).thenApply((res: InitializeResult) => {
          initializeResult = res
          LOG.info("Got initializeResult for " + rootPath)
          res
        })
        initializeStartTime = System.currentTimeMillis
        started = true
      }
      catch {
        case ex: Exception =>
          LOG.error(ex)
          stop()
      }
    }
  }

  /**
    * @return whether the underlying connection to language languageServer is still active
    */
  def isActive: Boolean = this.launcherFuture != null && !this.launcherFuture.isDone && !this.launcherFuture.isCancelled

  /**
    * Connects an editor to the languageServer
    *
    * @param editor the editor
    */
  @throws[IOException]
  def connect(editor: Editor): Unit = {
    val uri = Utils.editorToURIString(editor)
    uriToLanguageServerWrapper.put(uri, this)
    editorToLanguageServerWrapper.put(editor, this)
    if (!this.connectedEditors.contains(uri)) {
      start()
      if (this.initializeFuture != null && editor != null) {
        initializeFuture.thenRun(() => {
          if (!this.connectedEditors.contains(uri)) {
            val syncOptions: Either[TextDocumentSyncKind, TextDocumentSyncOptions] = if (initializeFuture == null) null else initializeResult.getCapabilities.getTextDocumentSync
            var syncKind: TextDocumentSyncKind = null
            if (syncOptions != null) {
              if (syncOptions.isRight) syncKind = syncOptions.getRight.getChange
              else if (syncOptions.isLeft) syncKind = syncOptions.getLeft
              val mouseListener = new EditorMouseListenerImpl
              val mouseMotionListener = new EditorMouseMotionListenerImpl
              val documentListener = new DocumentListenerImpl
              val selectionListener = new SelectionListenerImpl
              val manager = new EditorEventManager(editor, mouseListener, mouseMotionListener, documentListener, selectionListener, requestManager, syncKind, this)
              mouseListener.setManager(manager)
              mouseMotionListener.setManager(manager)
              documentListener.setManager(manager)
              selectionListener.setManager(manager)
              this.connectedEditors.put(uri, manager)
              LOG.info("Created a manager for " + uri)
            }
          }

        })
      } else {
        LOG.error(if (editor == null) "editor is null" else "initializeFuture is null")
      }
    }
  }

  /**
    * Disconnects an editor from the LanguageServer
    *
    * @param uri The uri of the editor
    */
  def disconnect(uri: String): Unit = {
    this.connectedEditors.remove(uri).foreach({ e =>
      uriToLanguageServerWrapper.remove(uri)
      editorToLanguageServerWrapper.remove(e.editor)
      e.editor.removeEditorMouseMotionListener(e.mouseMotionListener)
      e.editor.getDocument.removeDocumentListener(e.documentListener)
      e.documentClosed()
    })

    if (this.connectedEditors.isEmpty) stop()
  }

  /**
    * Checks if the wrapper is already connected to the document at the given path
    */
  def isConnectedTo(location: String): Boolean = connectedEditors.contains(location)

  /**
    * @return the LanguageServer
    */
  @Nullable def getServer: LanguageServer = {
    try
      start()
    catch {
      case ex: IOException =>
        LOG.error(ex)
    }
    if (initializeFuture != null && !this.initializeFuture.isDone) this.initializeFuture.join
    this.languageServer
  }

  /**
    * Warning: this is a long running operation
    *
    * @return the languageServer capabilities, or null if initialization job didn't complete
    */
  @Nullable def getServerCapabilities: ServerCapabilities = {
    try {
      start()
      if (this.initializeFuture != null) this.initializeFuture.get(if (capabilitiesAlreadyRequested) 0
      else 1000, TimeUnit.MILLISECONDS)
    } catch {
      case e: TimeoutException =>
        if (System.currentTimeMillis - initializeStartTime > 10000) LOG.error("LanguageServer not initialized after 10s", e) //$NON-NLS-1$
      case e@(_: IOException | _: InterruptedException | _: ExecutionException) =>
        LOG.error(e)
    }
    this.capabilitiesAlreadyRequested = true
    if (this.initializeResult != null) this.initializeResult.getCapabilities
    else null
  }

  /**
    * @return The language ID that this wrapper is dealing with if defined in the content type mapping for the language languageServer
    */
  @Nullable def getLanguageId(contentTypes: Array[String]): String = {
    if (contentTypes.exists(serverDefinition.getMappedExtensions.contains(_))) serverDefinition.id else null
  }

  def logMessage(message: Message): Unit = {
    message match {
      case responseMessage: ResponseMessage if responseMessage.getError != null && (responseMessage.getId eq Integer.toString(ResponseErrorCode.RequestCancelled.getValue)) =>
        LOG.error(new ResponseErrorException(responseMessage.getError))
      case _ =>
    }
  }

  def stop(): Unit = {
    if (this.initializeFuture != null) {
      this.initializeFuture.cancel(true)
      this.initializeFuture = null
    }
    this.initializeResult = null
    this.capabilitiesAlreadyRequested = false
    if (this.languageServer != null) try {
      val shutdown: CompletableFuture[AnyRef] = this.languageServer.shutdown
      shutdown.get(Timeout.SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)
    } catch {
      case _: Exception =>

      // most likely closed externally
    }
    if (this.launcherFuture != null) {
      this.launcherFuture.cancel(true)
      this.launcherFuture = null
    }
    if (this.lspStreamProvider != null) this.lspStreamProvider.stop()
    connectedEditors.foreach(e => disconnect(e._1))
    this.languageServer = null
    started = false
    PluginMain.languageServerStopped(this)
  }


}
