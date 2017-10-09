/** *****************************************************************************
  * Copyright (c) 2016, 2017 Red Hat Inc. and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * Mickael Istria (Red Hat Inc.) - initial implementation
  * Miro Spoenemann (TypeFox) - extracted LanguageClientImpl
  * Jan Koehnlein (TypeFox) - bug 521744
  * ******************************************************************************/
package com.github.gtache.client

import java.io.{File, IOException}
import java.net.URI
import java.util.concurrent._

import com.github.gtache.Utils
import com.github.gtache.editor.{EditorEventManager, EditorMouseMotionListenerImpl}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.{Either, Message, ResponseErrorCode, ResponseMessage}
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.{LanguageClient, LanguageServer}
import org.jetbrains.annotations.Nullable

import scala.collection.mutable

class LanguageServerWrapper(val serverDefinition: LanguageServerDefinition, val commands: Seq[String], val workingDir: String) {

  private val lspStreamProvider: StreamConnectionProvider = serverDefinition.createConnectionProvider(commands, workingDir)
  private val connectedEditors: mutable.Map[String, EditorEventManager] = mutable.HashMap()
  private val LOG: Logger = Logger.getInstance(classOf[LanguageServerWrapper])
  private var languageServer: LanguageServer = _
  private var client: LanguageClientImpl = _
  private var requestManager: RequestManager = _
  private var initializeResult: InitializeResult = _
  private var launcherFuture: Future[_] = _
  private var initializeFuture: CompletableFuture[InitializeResult] = _
  private var capabilitiesAlreadyRequested = false
  private var initializeStartTime = 0L
  private var started: Boolean = false

  /**
    * Returns the EditorEventManager for a given uri
    *
    * @param uri the URI as a string
    * @return the EditorEventManager (or null)
    */
  def getManagerFor(uri: String): EditorEventManager = {
    connectedEditors.get(uri).orNull
  }

  /**
    * Starts the LanguageServer
    */
  @throws[IOException]
  def start(rootFolder: String): Unit = {
    if (!started) {
      try {
        this.lspStreamProvider.start()
        client = serverDefinition.createLanguageClient
        val executorService = Executors.newCachedThreadPool
        val initParams = new InitializeParams
        initParams.setRootUri(new File(rootFolder).toURI.toString)
        //initParams.setRootPath(project.getLocation.toFile.getAbsolutePath)
        /*val launcher = LSPLauncher.createClientLauncher(client, this.lspStreamProvider.getInputStream, this.lspStreamProvider.getOutputStream, executorService, (consumer: MessageConsumer) => (message: Message) => {
        consumer.consume(message)
        logMessage(message)
        this.lspStreamProvider.handleMessage(message, this.languageServer, URI.create(initParams.getRootUri))
      })
      */
        val launcher = LSPLauncher.createClientLauncher(client, this.lspStreamProvider.getInputStream, this.lspStreamProvider.getOutputStream)

        this.languageServer = launcher.getRemoteProxy
        client.connect(languageServer)
        requestManager = new SimpleRequestManager(languageServer, client)
        this.launcherFuture = launcher.startListening
        val name = "Intellij" //$NON-NLS-1$
        val workspaceClientCapabilites = new WorkspaceClientCapabilities
        workspaceClientCapabilites.setApplyEdit(true)
        workspaceClientCapabilites.setExecuteCommand(new ExecuteCommandCapabilities)
        workspaceClientCapabilites.setSymbol(new SymbolCapabilities)
        val textDocumentClientCapabilities = new TextDocumentClientCapabilities
        textDocumentClientCapabilities.setCodeAction(new CodeActionCapabilities)
        textDocumentClientCapabilities.setCodeLens(new CodeLensCapabilities)
        textDocumentClientCapabilities.setCompletion(new CompletionCapabilities(new CompletionItemCapabilities(true)))
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
        textDocumentClientCapabilities.setSynchronization(new SynchronizationCapabilities(true, true, true))
        initParams.setCapabilities(new ClientCapabilities(workspaceClientCapabilites, textDocumentClientCapabilities, null))
        initParams.setClientName(name)
        initParams.setInitializationOptions(this.lspStreamProvider.getInitializationOptions(URI.create(initParams.getRootUri)))
        initializeFuture = languageServer.initialize(initParams).thenApply((res: InitializeResult) => {
          initializeResult = res
          LOG.info("Got initializeResult")
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
    val path = Utils.editorToURIString(editor)
    if (!this.connectedEditors.contains(path)) {
      start(Utils.editorToProjectFolderPath(editor))
      if (this.initializeFuture != null && editor != null) {
        initializeFuture.thenRun(() => {
          if (!this.connectedEditors.contains(path)) {
            val syncOptions: Either[TextDocumentSyncKind, TextDocumentSyncOptions] = if (initializeFuture == null) null else initializeResult.getCapabilities.getTextDocumentSync
            var syncKind: TextDocumentSyncKind = null
            if (syncOptions != null) {
              if (syncOptions.isRight) syncKind = syncOptions.getRight.getChange
              else if (syncOptions.isLeft) syncKind = syncOptions.getLeft
              val listener = new EditorMouseMotionListenerImpl()
              val manager = new EditorEventManager(editor, listener, requestManager, syncKind)
              listener.setManager(manager)
              requestManager.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(Utils.editorToURIString(editor), serverDefinition.id, 0, editor.getDocument.getText)))
              LanguageServerWrapper.this.connectedEditors.put(path, manager)
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
    * @param path The uri of the editor
    */
  def disconnect(path: String): Unit = {
    this.connectedEditors.remove(path).foreach({ e =>
      e.editor.removeEditorMouseMotionListener(e.mouseMotionListener)
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
      start(".")
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
      start(".")
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

  private def logMessage(message: Message): Unit = {
    message match {
      case responseMessage: ResponseMessage if responseMessage.getError != null && (responseMessage.getId eq Integer.toString(ResponseErrorCode.RequestCancelled.getValue)) =>
        LOG.error(new ResponseErrorException(responseMessage.getError))
      case _ =>
    }
  }

  private def stop(): Unit = {
    if (this.initializeFuture != null) {
      this.initializeFuture.cancel(true)
      this.initializeFuture = null
    }
    this.initializeResult = null
    this.capabilitiesAlreadyRequested = false
    if (this.languageServer != null) try {
      val shutdown: CompletableFuture[AnyRef] = this.languageServer.shutdown
      shutdown.get(5000, TimeUnit.MILLISECONDS)
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
  }


}
