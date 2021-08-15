/* Adapted from lsp4e */
package com.github.gtache.lsp.client.languageserver.wrapper

import com.github.gtache.lsp.client.DynamicRegistrationMethods
import com.github.gtache.lsp.client.LanguageClientImpl
import com.github.gtache.lsp.client.languageserver.*
import com.github.gtache.lsp.client.languageserver.requestmanager.RequestManager
import com.github.gtache.lsp.client.languageserver.requestmanager.SimpleRequestManager
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import com.github.gtache.lsp.editor.EditorEventManager
import com.github.gtache.lsp.editor.listeners.DocumentListenerImpl
import com.github.gtache.lsp.editor.listeners.EditorMouseListenerImpl
import com.github.gtache.lsp.editor.listeners.EditorMouseMotionListenerImpl
import com.github.gtache.lsp.editor.listeners.SelectionListenerImpl
import com.github.gtache.lsp.head
import com.github.gtache.lsp.multicatch
import com.github.gtache.lsp.requests.Timeout
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.reversed
import com.github.gtache.lsp.settings.LSPProjectState
import com.github.gtache.lsp.settings.server.LSPConfiguration
import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.FileUtils
import com.github.gtache.lsp.utils.LSPException
import com.google.gson.JsonObject
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer
import org.jetbrains.annotations.Nullable
import java.io.*
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.*


/**
 * The implementation of a LanguageServerWrapper (specific to a serverDefinition and a project)
 *
 * @param serverDefinition The serverDefinition
 * @param project          The project
 */
class LanguageServerWrapperImpl(
    override val serverDefinition: LanguageServerDefinition,
    override val project: Project
) : LanguageServerWrapper {

    companion object {
        private val logger: Logger = Logger.getInstance(LanguageServerWrapperImpl::class.java)
        private val uriToLanguageServerWrapper: MutableMap<Pair<String, String>, LanguageServerWrapper> = HashMap()

        /**
         * @param uri A file uri
         * @return The wrapper for the given uri, or None
         */
        fun forUri(uri: String, project: Project): LanguageServerWrapper? {
            return uriToLanguageServerWrapper[Pair(uri, FileUtils.projectToUri(project))]
        }

        /**
         * @param editor An editor
         * @return The wrapper for the given editor, or None
         */
        fun forEditor(editor: Editor): LanguageServerWrapper? {
            return if (editor.project == null) {
                null
            } else {
                uriToLanguageServerWrapper[Pair(
                    FileUtils.editorToURIString(editor),
                    FileUtils.editorToProjectFolderUri(editor)
                )]
            }
        }
    }

    private val toConnect: MutableSet<Editor> = HashSet()
    private val rootPath = project.basePath
    private val connectedEditors: MutableMap<String, EditorEventManager> = HashMap()
    private val statusWidget: LSPServerStatusWidget? = LSPServerStatusWidget.widgets[project]
    private val registrations: MutableMap<String, DynamicRegistrationMethods> = HashMap()
    private var crashCount = 0

    private val factory = project.service<StatusBarWidgetsManager>().widgetFactories.filterIsInstance(LSPServerStatusWidgetFactory::class.java).head

    init {
        factory.addWrapper(this)
    }

    @Volatile
    private var alreadyShownTimeout = false

    @Volatile
    private var alreadyShownCrash = false

    @Volatile
    override var status: ServerStatus = ServerStatus.STOPPED
        set(newStatus) {
            field = newStatus
            statusWidget?.statusUpdated(this)
        }

    override var languageServer: LanguageServer? = null
    override var requestManager: RequestManager? = null
    override var configuration: LSPConfiguration? = null
        set(newConfiguration) {
            if (newConfiguration != null) {
                if (newConfiguration.isValid()) {
                    field = newConfiguration
                    requestManager?.didChangeConfiguration(
                        DidChangeConfigurationParams(
                            newConfiguration.getAttributesForSectionAndUri(
                                "",
                                "global"
                            )
                        )
                    )
                }
            }
        }

    private var client: LanguageClientImpl? = null
    private var initializeResult: InitializeResult? = null
    private var launcherFuture: Future<Void>? = null
    private var initializeFuture: CompletableFuture<InitializeResult>? = null
    private var capabilitiesAlreadyRequested = false
    private var initializeStartTime = 0L
    private var errLogThread: Thread? = null
    private var fileWatchers: Iterable<FileSystemWatcher> = emptyList()

    /**
     * @return if the server supports willSaveWaitUntil
     */
    fun isWillSaveWaitUntil(): Boolean {
        val capabilities = getServerCapabilities()?.textDocumentSync
        return if (capabilities != null) {
            if (capabilities.isLeft) {
                false
            } else {
                capabilities.right.willSaveWaitUntil
            }
        } else {
            false
        }
    }

    private fun getAllPotentialEditors(): List<Editor> {
        return FileUtils.getAllOpenedEditors(project).filter { e -> serverDefinition.mappedExtensions.contains(FileUtils.extFromEditor(e)) }
    }

    /**
     * Warning: this is a long running operation
     *
     * @return the languageServer capabilities, or null if initialization job didn't complete
     */
    override fun getServerCapabilities(): ServerCapabilities? {
        if (initializeResult != null) return initializeResult?.capabilities else {
            try {
                start()
                if (initializeFuture != null) initializeFuture?.get(
                    if (capabilitiesAlreadyRequested) 0L else Timeout.INIT_TIMEOUT().toLong(),
                    TimeUnit.MILLISECONDS
                )
                notifySuccess(Timeouts.INIT)
            } catch (e: TimeoutException) {
                notifyFailure(Timeouts.INIT)
                val msg = "LanguageServer for definition\n " + serverDefinition + "\nnot initialized after " + Timeout.INIT_TIMEOUT() / 1000 + "s\nCheck settings"
                logger.warn(msg, e)
                ApplicationUtils.invokeLater {
                    if (!alreadyShownTimeout) {
                        Messages.showErrorDialog(msg, "LSP error")
                        alreadyShownTimeout = true
                    }
                }
                stop()
            } catch (e: Exception) {
                e.multicatch(IOException::class, InterruptedException::class, ExecutionException::class) {
                    logger.warn(e)
                    stop()
                }
            }
            capabilitiesAlreadyRequested = true
            return if (initializeResult != null) initializeResult?.capabilities
            else null
        }
    }

    override fun getServer(): LanguageServer? {
        start()
        if (initializeFuture != null && !initializeFuture!!.isDone) initializeFuture!!.join()
        return languageServer
    }

    override fun notifyResult(timeouts: Timeouts, success: Boolean): Unit {
        statusWidget?.notifyResult(timeouts, success)
    }

    /**
     * Returns the EditorEventManager for a given uri
     *
     * @param uri the URI as a string
     * @return the EditorEventManager (or null)
     */
    override fun getEditorManagerFor(uri: String): EditorEventManager? {
        return connectedEditors[uri]
    }


    /**
     * @return whether the underlying connection to language languageServer is still active
     */
    override fun isActive(): Boolean = (launcherFuture?.let { !it.isDone && !it.isCancelled } ?: false) && !alreadyShownTimeout && !alreadyShownCrash

    /**
     * Connects an editor to the languageServer
     *
     * @param editor the editor
     */
    override fun connect(editor: Editor): Unit {
        val uri = FileUtils.editorToURIString(editor)
        if (uri != null) {
            synchronized(uriToLanguageServerWrapper) {
                val projectUri = FileUtils.editorToProjectFolderUri(editor)
                if (projectUri != null) {
                    uriToLanguageServerWrapper[Pair(uri, projectUri)] = this
                } else {
                    logger.warn("Null projectFolder for $editor")
                }
            }
            if (!this.connectedEditors.contains(uri)) {
                start()
                if (this.initializeFuture != null) {
                    val capabilities = getServerCapabilities()
                    if (capabilities != null) {
                        initializeFuture!!.thenRun {
                            if (!this.connectedEditors.contains(uri)) {
                                try {
                                    val syncOptions: Either<TextDocumentSyncKind, TextDocumentSyncOptions>? = capabilities.textDocumentSync
                                    if (syncOptions != null) {
                                        val syncKind = if (syncOptions.isRight) syncOptions.right.change
                                        else if (syncOptions.isLeft) syncOptions.left
                                        else DummyServerOptions.SYNC_KIND
                                        val mouseListener = EditorMouseListenerImpl()
                                        val mouseMotionListener = EditorMouseMotionListenerImpl()
                                        val documentListener = DocumentListenerImpl()
                                        val selectionListener = SelectionListenerImpl()
                                        val renameProvider = capabilities.renameProvider
                                        val renameOptions = if (renameProvider != null) {
                                            if (renameProvider.isLeft) {
                                                if (renameProvider.left) RenameOptions() else DummyServerOptions.RENAME
                                            } else {
                                                renameProvider.right
                                            }
                                        } else DummyServerOptions.RENAME
                                        val serverOptions = ServerOptions(
                                            syncKind,
                                            capabilities.completionProvider ?: DummyServerOptions.COMPLETION,
                                            capabilities.signatureHelpProvider ?: DummyServerOptions.SIGNATURE_HELP,
                                            capabilities.codeLensProvider ?: DummyServerOptions.CODELENS,
                                            capabilities.documentOnTypeFormattingProvider ?: DummyServerOptions.DOCUMENT_ON_TYPE_FORMATTING,
                                            capabilities.documentLinkProvider ?: DummyServerOptions.DOCUMENT_LINK,
                                            capabilities.executeCommandProvider ?: DummyServerOptions.EXECUTE_COMMAND,
                                            capabilities.semanticHighlighting ?: DummyServerOptions.SEMANTIC_HIGHLIGHTING,
                                            renameOptions
                                        )
                                        val manager = EditorEventManager(
                                            editor,
                                            mouseListener,
                                            mouseMotionListener,
                                            documentListener,
                                            selectionListener,
                                            requestManager!!,
                                            serverOptions,
                                            this
                                        )
                                        mouseListener.manager = manager
                                        mouseMotionListener.manager = manager
                                        documentListener.manager = manager
                                        selectionListener.manager = manager
                                        manager.registerListeners()
                                        synchronized(connectedEditors) {
                                            this.connectedEditors.put(uri, manager)
                                        }
                                        manager.documentOpened()
                                        logger.info("Created a manager for $uri")
                                        toConnect.remove(editor)
                                        toConnect.forEach { e -> connect(e) }
                                    }
                                } catch (e: Exception) {
                                    logger.error(e)
                                }
                            }

                        }
                    } else {
                        logger.warn("Capabilities are null for $serverDefinition")
                    }
                } else {
                    synchronized(toConnect) {
                        toConnect.add(editor)
                    }
                }
            }
        } else logger.warn("Null uri for $editor")
    }

    /**
     * Disconnects an editor from the LanguageServer
     *
     * @param path The uri of the editor
     */
    override fun disconnect(path: String): Unit {
        synchronized(connectedEditors) {
            synchronized(uriToLanguageServerWrapper) {
                this.connectedEditors.remove(path)?.let { e ->
                    uriToLanguageServerWrapper.remove(Pair(path, FileUtils.projectToUri(project)))
                    e.removeListeners()
                    e.documentClosed()
                }
            }
        }

        if (this.connectedEditors.isEmpty()) stop()
    }

    override fun stop(): Unit {
        initializeFuture?.let {
            if (!it.isCancelled) {
                try {
                    it.cancel(true)
                } catch (e: CancellationException) {
                    logger.warn(e)
                }
            }
            initializeFuture = null
        }
        initializeResult = null
        capabilitiesAlreadyRequested = false
        languageServer?.let {
            try {
                val shutdown: CompletableFuture<Any> = it.shutdown()
                shutdown.get(Timeout.SHUTDOWN_TIMEOUT(), TimeUnit.MILLISECONDS)
                notifySuccess(Timeouts.SHUTDOWN)
            } catch (e: Exception) {
                notifyFailure(Timeouts.SHUTDOWN)
                // most likely closed externally
            }
        }
        launcherFuture?.let {
            if (!it.isCancelled) it.cancel(true)
            this.launcherFuture = null
        }
        if (rootPath != null) serverDefinition.stop(rootPath)
        connectedEditors.forEach { e -> disconnect(e.key) }
        languageServer = null
        status = ServerStatus.STOPPED
        stopLoggingServerErrors()
    }

    /**
     * Checks if the wrapper is already connected to the document at the given path
     */
    override fun isConnectedTo(location: String): Boolean = connectedEditors.contains(location)

    private fun prepareWorkspaceClientCapabilities(): WorkspaceClientCapabilities {
        val workspaceClientCapabilities = WorkspaceClientCapabilities()
        workspaceClientCapabilities.applyEdit = true
        workspaceClientCapabilities.didChangeConfiguration = DidChangeConfigurationCapabilities()
        workspaceClientCapabilities.didChangeWatchedFiles = DidChangeWatchedFilesCapabilities(true)
        workspaceClientCapabilities.executeCommand = ExecuteCommandCapabilities()
        val wec = WorkspaceEditCapabilities()
        //TODO set failureHandling and resourceOperations
        wec.documentChanges = true
        workspaceClientCapabilities.workspaceEdit = wec
        workspaceClientCapabilities.symbol = SymbolCapabilities()
        workspaceClientCapabilities.workspaceFolders = false
        workspaceClientCapabilities.configuration = true
        return workspaceClientCapabilities
    }

    private fun prepareTextDocumentClientCapabilities(): TextDocumentClientCapabilities {
        val textDocumentClientCapabilities = TextDocumentClientCapabilities()
        textDocumentClientCapabilities.codeAction = CodeActionCapabilities()
        //textDocumentClientCapabilities.setCodeLens(new CodeLensCapabilities)
        //textDocumentClientCapabilities.setColorProvider(new ColorProviderCapabilities)
        textDocumentClientCapabilities.completion = CompletionCapabilities(CompletionItemCapabilities(true))
        textDocumentClientCapabilities.definition = DefinitionCapabilities()
        textDocumentClientCapabilities.documentHighlight = DocumentHighlightCapabilities()
        //textDocumentClientCapabilities.setDocumentLink(new DocumentLinkCapabilities)
        //textDocumentClientCapabilities.setDocumentSymbol(new DocumentSymbolCapabilities)
        //textDocumentClientCapabilities.setFoldingRange(new FoldingRangeCapabilities)
        textDocumentClientCapabilities.formatting = FormattingCapabilities()
        textDocumentClientCapabilities.hover = HoverCapabilities()
        //textDocumentClientCapabilities.setImplementation(new ImplementationCapabilities)
        textDocumentClientCapabilities.onTypeFormatting = OnTypeFormattingCapabilities()
        textDocumentClientCapabilities.rangeFormatting = RangeFormattingCapabilities()
        textDocumentClientCapabilities.references = ReferencesCapabilities()
        textDocumentClientCapabilities.rename = RenameCapabilities(true, false)
        textDocumentClientCapabilities.semanticHighlightingCapabilities = SemanticHighlightingCapabilities(false)
        textDocumentClientCapabilities.signatureHelp = SignatureHelpCapabilities()
        textDocumentClientCapabilities.synchronization = SynchronizationCapabilities(true, true, true)
        //textDocumentClientCapabilities.setTypeDefinition(new TypeDefinitionCapabilities)
        return textDocumentClientCapabilities
    }

    /**
     * Starts the LanguageServer
     */
    override fun start(): Unit {
        if (status == ServerStatus.STOPPED || status == ServerStatus.FAILED) {
            status = ServerStatus.STARTING
            if (rootPath != null) {
                try {
                    val (inputStream, outputStream) = serverDefinition.start(rootPath)
                    startLoggingServerErrors()
                    loadConfiguration()
                    client = serverDefinition.createLanguageClient()
                    client?.let { c ->
                        val initParams = InitializeParams()
                        initParams.rootUri = FileUtils.pathToUri(rootPath)
                        val outWriter = getOutWriter()

                        val launcher =
                            if (project.service<LSPProjectState>().isLoggingServersOutput) LSPLauncher.createClientLauncher(
                                c,
                                inputStream,
                                outputStream,
                                false,
                                outWriter
                            )
                            else LSPLauncher.createClientLauncher(c, inputStream, outputStream)

                        languageServer = launcher.remoteProxy
                        languageServer?.let { ls ->
                            c.connect(ls, this)
                            launcherFuture = launcher.startListening()
                            //TODO update capabilities when implemented
                            val workspaceClientCapabilities = prepareWorkspaceClientCapabilities()
                            val textDocumentClientCapabilities = prepareTextDocumentClientCapabilities()
                            initParams.capabilities = ClientCapabilities(
                                workspaceClientCapabilities,
                                textDocumentClientCapabilities,
                                null
                            )
                            initParams.initializationOptions = this.serverDefinition.getInitializationOptions(URI.create(initParams.rootUri))

                            initializeFuture = ls.initialize(initParams).thenApply { res ->
                                initializeResult = res
                                logger.info("Got initializeResult for $serverDefinition ; $rootPath")
                                status = ServerStatus.STARTED
                                requestManager = SimpleRequestManager(this, ls, c, res.capabilities)
                                requestManager?.initialized(InitializedParams())
                                configuration?.let {
                                    requestManager?.didChangeConfiguration(DidChangeConfigurationParams(it.getAttributesForSectionAndUri("", "global")))
                                }
                                res
                            }
                            initializeStartTime = System.currentTimeMillis()
                        }
                    }
                } catch (e: Exception) {
                    e.multicatch(LSPException::class, IOException::class) {
                        logger.warn(e)
                        ApplicationUtils.invokeLater { Messages.showErrorDialog("Can't start server, please check settings\n" + e.message, "LSP Error") }
                        setFailed()
                        stop()
                    }
                }
            } else logger.warn("RootPath is null")
        }
    }

    override fun restart(): Unit {
        if (status == ServerStatus.STARTED || status == ServerStatus.STARTING) {
            logger.info("Stopping $serverDefinition for restart")
            stop()
        }
        getAllPotentialEditors().forEach { connect(it) }
    }

    /**
     * @return The language ID that this wrapper is dealing , if defined in the content type mapping for the language languageServer
     */
    @Nullable
    override fun getLanguageId(contentTypes: Array<String>): String? {
        return if (contentTypes.any { serverDefinition.mappedExtensions.contains(it) }) serverDefinition.id else null
    }

    override fun logMessage(message: Message): Unit {
        if (message is ResponseMessage) {
            if (message.error != null && message.id == ResponseErrorCode.RequestCancelled.value.toString()) {
                logger.error(ResponseErrorException(message.error))
            }
        } else {
            logger.info("Got an unknown message type : $message")
        }
    }

    override fun registerCapability(params: RegistrationParams): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            params.registrations.forEach { r ->
                val id = r.id
                val method = DynamicRegistrationMethods.forName(r.method)
                if (method != null) {
                    val options = r.registerOptions
                    registrations[id] = method
                    if (method == DynamicRegistrationMethods.DID_CHANGE_WATCHED_FILES) {
                        when (options) {
                            is DidChangeWatchedFilesRegistrationOptions -> fileWatchers = options.watchers
                            is JsonObject -> try {
                                val watchers = options.getAsJsonArray("watchers")
                                fileWatchers = (0.until(watchers.size())).map { i ->
                                    val watcher = watchers.get(i) as JsonObject
                                    FileSystemWatcher(
                                        watcher.getAsJsonPrimitive("globPattern").asString,
                                        watcher.getAsJsonPrimitive("kind").asInt
                                    )
                                }
                            } catch (e: Exception) {
                                logger.warn(e)
                            }
                            else -> logger.warn("Mismatched options type : expected DidChangeWatchedFilesRegistrationOptions, got " + options.javaClass)
                        }
                    }
                }
            }
        }
    }

    override fun unregisterCapability(params: UnregistrationParams): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            params.unregisterations.forEach { r ->
                val id = r.id
                val method = DynamicRegistrationMethods.forName(r.method)
                if (method != null) {
                    if (registrations.contains(id)) {
                        registrations.remove(id)
                    } else {
                        val invert = registrations.reversed()
                        if (invert.containsKey(method)) {
                            registrations.remove(invert[method])
                        }
                    }
                    if (method == DynamicRegistrationMethods.DID_CHANGE_WATCHED_FILES) {
                        fileWatchers = emptyList()
                    }
                }

            }
        }
    }

    override fun crashed(e: Exception): Unit {
        crashCount += 1
        if (crashCount < 4) {
            stop()
            val editors = HashMap(connectedEditors).keys
            editors.forEach { uri -> connect(uri) }
        } else {
            setFailed()
            stop()
            if (!alreadyShownCrash) ApplicationUtils.invokeLater {
                if (!alreadyShownCrash)
                    Messages.showErrorDialog(
                        "LanguageServer for definition " + serverDefinition + ", project " + project + " keeps crashing due to \n" + e.message + "\nCheck settings.",
                        "LSP Error"
                    )
                alreadyShownCrash = true
            }
        }
    }

    override fun getConnectedFiles(): Iterable<String> {
        return connectedEditors.keys.map { s -> URI(FileUtils.sanitizeURI(s)).toString() }
    }

    override fun removeWidget(): Unit {
        factory.removeWrapper(this)
        statusWidget?.dispose()
    }

    /**
     * Disconnects an editor from the LanguageServer
     *
     * @param editor The editor
     */
    override fun disconnect(editor: Editor): Unit {
        val uri = FileUtils.editorToURIString(editor)
        if (uri != null) {
            disconnect(uri)
        } else {
            logger.warn("Null uri for $editor")
        }
    }

    private fun setFailed(): Unit {
        status = ServerStatus.FAILED
    }

    private fun connect(uri: String): Unit {
        val vfs = FileUtils.URIToVFS(uri)
        if (vfs != null) {
            val editors = FileEditorManager.getInstance(project).getAllEditors(vfs)
                .mapNotNull { t -> (t as TextEditor).editor }
            if (editors.isNotEmpty()) {
                connect(editors.head)
            }
        } else {
            logger.warn("No VFS for $uri")
        }
    }

    private fun startLoggingServerErrors(): Unit {
        data class ReaderPrinterRunnable(val inPath: InputStream, val outPath: String) : Runnable {
            override fun run(): Unit {
                var notInterrupted = true
                val scanner = Scanner(inPath)
                val out = File(outPath)
                val writer = BufferedWriter(FileWriter(out, true))
                while (scanner.hasNextLine() && notInterrupted) {
                    if (!Thread.currentThread().isInterrupted) {
                        writer.write(scanner.nextLine() + "\n")
                        writer.flush()
                    } else {
                        notInterrupted = false
                        writer.close()
                    }
                }
            }
        }

        if (rootPath != null) {
            serverDefinition.getOutputStreams(rootPath)?.second?.let {
                val errRunnable = ReaderPrinterRunnable(it, getLogPath("err"))
                errLogThread = Thread(errRunnable)
                errLogThread?.start()
            } ?: run { logger.warn("Null error stream for $rootPath") }
        } else logger.warn("Null rootpath")
    }

    private fun loadConfiguration(): Unit {
        val file = File(getConfPath())
        if (!file.exists()) {
            file.createNewFile()
        }
        configuration = LSPConfiguration.fromFile(file)
    }

    private fun getConfPath(): String {
        val dir = rootPath + "/" + FileUtils.LSP_CONFIG_DIR
        File(dir).mkdirs()
        return rootPath + "/" + FileUtils.LSP_CONFIG_DIR + serverDefinition.id.replace(";", "_") + ".json"
    }

    private fun getLogPath(suffix: String): String {
        val dir = rootPath + "/" + FileUtils.LSP_LOG_DIR
        File(dir).mkdirs()
        val date = SimpleDateFormat("yyyyMMdd").format(Date())
        val basename = dir + serverDefinition.id.replace(";", "_")
        return basename + "_" + suffix + "_" + date + ".log"
    }

    private fun getOutWriter(): PrintWriter {
        return PrintWriter(FileWriter(File(getLogPath("out")), true))
    }

    private fun stopLoggingServerErrors(): Unit {
        errLogThread?.interrupt()
    }

    override fun didChangeWatchedFiles(uri: String, typ: FileChangeType): Unit {
        val params = DidChangeWatchedFilesParams(listOf(FileEvent(uri, typ)))
        val uriFile = File(URI(uri))
        val confFile = File(getConfPath())
        try {
            if (uriFile.exists() && confFile.exists() && Files.isSameFile(uriFile.toPath(), confFile.toPath())) {
                configuration = LSPConfiguration.fromFile(confFile)
            }
        } catch (e: Exception) {
            logger.warn(e)
        } catch (t: Throwable) {

        }
        if (registrations.values.toSet().contains(DynamicRegistrationMethods.DID_CHANGE_WATCHED_FILES)) {
            if (fileWatchers.any { fw ->
                    val pattern = fw.globPattern
                    val event = fw.kind
                    val typInt = when (typ) {
                        FileChangeType.Created -> 1
                        FileChangeType.Changed -> 2
                        FileChangeType.Deleted -> 4
                    }
                    (event and typInt) != 0 && FileSystems.getDefault().getPathMatcher("glob:$pattern")
                        .matches(Paths.get(URI(uri)))
                }) {
                requestManager?.didChangeWatchedFiles(params)
            }
        } else {
            //If the server didn't register for file events, send anyway
            requestManager?.didChangeWatchedFiles(params)
        }
    }
}