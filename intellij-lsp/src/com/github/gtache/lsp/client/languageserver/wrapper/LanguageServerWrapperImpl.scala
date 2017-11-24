/* Adapted from lsp4e */
package com.github.gtache.lsp.client.languageserver.wrapper

import java.io.IOException
import java.net.URI
import java.util.concurrent._

import com.github.gtache.lsp.client.connection.StreamConnectionProvider
import com.github.gtache.lsp.client.languageserver.ServerOptions
import com.github.gtache.lsp.client.languageserver.requestmanager.{RequestManager, SimpleRequestManager}
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.client.{DynamicRegistrationMethods, LanguageClientImpl}
import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.editor.listeners.{DocumentListenerImpl, EditorMouseListenerImpl, EditorMouseMotionListenerImpl, SelectionListenerImpl}
import com.github.gtache.lsp.requests.Timeout
import com.github.gtache.lsp.utils.FileUtils
import com.github.gtache.lsp.{LSPServerStatusWidget, ServerStatus}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.{Either, Message, ResponseErrorCode, ResponseMessage}
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer
import org.jetbrains.annotations.Nullable

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

object LanguageServerWrapperImpl {
  private val uriToLanguageServerWrapper: mutable.Map[String, LanguageServerWrapper] = TrieMap()
  private val editorToLanguageServerWrapper: mutable.Map[Editor, LanguageServerWrapper] = TrieMap()

  /**
    * @param uri A file uri
    * @return The wrapper for the given uri, or None
    */
  def forUri(uri: String): Option[LanguageServerWrapper] = {
    uriToLanguageServerWrapper.get(uri)
  }

  /**
    * @param editor An editor
    * @return The wrapper for the given editor, or None
    */
  def forEditor(editor: Editor): Option[LanguageServerWrapper] = {
    editorToLanguageServerWrapper.get(editor)
  }
}

/**
  * The implementation of a LanguageServerWrapper (specific to a serverDefinition and a project)
  *
  * @param serverDefinition The serverDefinition
  * @param project          The project
  */
class LanguageServerWrapperImpl(val serverDefinition: LanguageServerDefinition, val project: Project) extends LanguageServerWrapper {

  import LanguageServerWrapperImpl._

  private val rootPath = project.getBasePath
  private val connectedEditors: mutable.Map[String, EditorEventManager] = mutable.HashMap()
  private val LOG: Logger = Logger.getInstance(classOf[LanguageServerWrapperImpl])
  private val statusWidget: LSPServerStatusWidget = LSPServerStatusWidget.createWidgetFor(this)
  private var status: ServerStatus = ServerStatus.STOPPED
  private var languageServer: LanguageServer = _
  private var client: LanguageClientImpl = _
  private var requestManager: RequestManager = _
  private var initializeResult: InitializeResult = _
  private var launcherFuture: Future[_] = _
  private var initializeFuture: CompletableFuture[InitializeResult] = _
  private var capabilitiesAlreadyRequested = false
  private var initializeStartTime = 0L
  private var started: Boolean = false
  private var registrations: mutable.Map[String, DynamicRegistrationMethods] = mutable.HashMap()


  override def getServerDefinition: LanguageServerDefinition = serverDefinition

  /**
    * @return if the server supports willSaveWaitUntil
    */
  def isWillSaveWaitUntil: Boolean = {
    val capabilities = getServerCapabilities.getTextDocumentSync
    if (capabilities.isLeft) {
      false
    } else {
      capabilities.getRight.getWillSaveWaitUntil
    }
  }

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
      serverDefinition.createConnectionProvider(rootPath)
      status = ServerStatus.STARTING
      statusWidget.setStatus(status)
      val (inputStream, outputStream) = serverDefinition.start()
      client = serverDefinition.createLanguageClient
      val initParams = new InitializeParams
      initParams.setRootUri(FileUtils.pathToUri(rootPath))
      val launcher = LSPLauncher.createClientLauncher(client, inputStream, outputStream)

      this.languageServer = launcher.getRemoteProxy
      client.connect(languageServer, this)
      this.launcherFuture = launcher.startListening
      //TODO update capabilities when implemented
      val workspaceClientCapabilities = new WorkspaceClientCapabilities
      workspaceClientCapabilities.setApplyEdit(true)
      //workspaceClientCapabilities.setDidChangeConfiguration(new DidChangeConfigurationCapabilities)
      workspaceClientCapabilities.setDidChangeWatchedFiles(new DidChangeWatchedFilesCapabilities)
      workspaceClientCapabilities.setExecuteCommand(new ExecuteCommandCapabilities)
      workspaceClientCapabilities.setWorkspaceEdit(new WorkspaceEditCapabilities(true))
      workspaceClientCapabilities.setSymbol(new SymbolCapabilities)
      val textDocumentClientCapabilities = new TextDocumentClientCapabilities
      textDocumentClientCapabilities.setCodeAction(new CodeActionCapabilities)
      //textDocumentClientCapabilities.setCodeLens(new CodeLensCapabilities)
      textDocumentClientCapabilities.setCompletion(new CompletionCapabilities(new CompletionItemCapabilities(false)))
      textDocumentClientCapabilities.setDefinition(new DefinitionCapabilities)
      textDocumentClientCapabilities.setDocumentHighlight(new DocumentHighlightCapabilities)
      //textDocumentClientCapabilities.setDocumentLink(new DocumentLinkCapabilities)
      //textDocumentClientCapabilities.setDocumentSymbol(new DocumentSymbolCapabilities)
      textDocumentClientCapabilities.setFormatting(new FormattingCapabilities)
      textDocumentClientCapabilities.setHover(new HoverCapabilities)
      textDocumentClientCapabilities.setOnTypeFormatting(new OnTypeFormattingCapabilities)
      textDocumentClientCapabilities.setRangeFormatting(new RangeFormattingCapabilities)
      textDocumentClientCapabilities.setReferences(new ReferencesCapabilities)
      textDocumentClientCapabilities.setRename(new RenameCapabilities)
      textDocumentClientCapabilities.setSignatureHelp(new SignatureHelpCapabilities)
      textDocumentClientCapabilities.setSynchronization(new SynchronizationCapabilities(true, true, true))
      initParams.setCapabilities(new ClientCapabilities(workspaceClientCapabilities, textDocumentClientCapabilities, null))
      initParams.setInitializationOptions(this.serverDefinition.getInitializationOptions(URI.create(initParams.getRootUri)))
      initializeFuture = languageServer.initialize(initParams).thenApply((res: InitializeResult) => {
        status = ServerStatus.STARTED
        statusWidget.setStatus(status)
        initializeResult = res
        LOG.info("Got initializeResult for " + rootPath)
        requestManager = new SimpleRequestManager(languageServer, client, getServerCapabilities)
        res
      })
      initializeStartTime = System.currentTimeMillis
      started = true
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
    val uri = FileUtils.editorToURIString(editor)
    uriToLanguageServerWrapper.put(uri, this)
    editorToLanguageServerWrapper.put(editor, this)
    if (!this.connectedEditors.contains(uri)) {
      start()
      if (this.initializeFuture != null && editor != null) {
        initializeFuture.thenRun(() => {
          if (!this.connectedEditors.contains(uri)) {
            val capabilities = getServerCapabilities
            val syncOptions: Either[TextDocumentSyncKind, TextDocumentSyncOptions] = if (initializeFuture == null) null else capabilities.getTextDocumentSync
            var syncKind: TextDocumentSyncKind = null
            if (syncOptions != null) {
              if (syncOptions.isRight) syncKind = syncOptions.getRight.getChange
              else if (syncOptions.isLeft) syncKind = syncOptions.getLeft
              val mouseListener = new EditorMouseListenerImpl
              val mouseMotionListener = new EditorMouseMotionListenerImpl
              val documentListener = new DocumentListenerImpl
              val selectionListener = new SelectionListenerImpl
              val serverOptions = ServerOptions(syncKind, capabilities.getCompletionProvider, capabilities.getSignatureHelpProvider, capabilities.getCodeLensProvider, capabilities.getDocumentOnTypeFormattingProvider, capabilities.getDocumentLinkProvider, capabilities.getExecuteCommandProvider)
              val manager = new EditorEventManager(editor, mouseListener, mouseMotionListener, documentListener, selectionListener, requestManager, serverOptions, this)
              mouseListener.setManager(manager)
              mouseMotionListener.setManager(manager)
              documentListener.setManager(manager)
              selectionListener.setManager(manager)
              manager.registerListeners()
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
      e.removeListeners()
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
    if (this.serverDefinition != null) this.serverDefinition.stop()
    connectedEditors.foreach(e => disconnect(e._1))
    this.languageServer = null
    started = false
    status = ServerStatus.STOPPED
    statusWidget.setStatus(status)
  }

  override def registerCapability(params: RegistrationParams): CompletableFuture[Void] = {
    CompletableFuture.runAsync(() => {
      import scala.collection.JavaConverters._
      params.getRegistrations.asScala.foreach(r => {
        val id = r.getId
        val method = DynamicRegistrationMethods.forName(r.getMethod)
        val options = r.getRegisterOptions
        registrations.put(id, method)
      })
    })
  }

  override def unregisterCapability(params: UnregistrationParams): CompletableFuture[Void] = {
    CompletableFuture.runAsync(() => {
      import scala.collection.JavaConverters._
      params.getUnregisterations.asScala.foreach(r => {
        val id = r.getId
        val method = DynamicRegistrationMethods.forName(r.getMethod)
        if (registrations.contains(id)) {
          registrations.remove(id)
        } else {
          val invert = registrations.map(mapping => (mapping._2, mapping._1))
          if (invert.contains(method)) {
            registrations.remove(invert(method))
          }
        }
      })
    })
  }

  override def getProject: Project = project

  override def getStatus: ServerStatus = status
}
