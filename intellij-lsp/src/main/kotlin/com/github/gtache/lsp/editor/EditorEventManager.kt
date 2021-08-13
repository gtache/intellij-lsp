package com.github.gtache.lsp.editor

import com.github.gtache.lsp.actions.LSPReferencesAction
import com.github.gtache.lsp.client.languageserver.DummyServerOptions
import com.github.gtache.lsp.client.languageserver.ServerOptions
import com.github.gtache.lsp.client.languageserver.requestmanager.RequestManager
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapperImpl
import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.contributors.rename.LSPRenameProcessor
import com.github.gtache.lsp.head
import com.github.gtache.lsp.multicatch
import com.github.gtache.lsp.requests.HoverHandler
import com.github.gtache.lsp.requests.SemanticHighlightingHandler
import com.github.gtache.lsp.requests.Timeout.CODEACTION_TIMEOUT
import com.github.gtache.lsp.requests.Timeout.COMPLETION_TIMEOUT
import com.github.gtache.lsp.requests.Timeout.DEFINITION_TIMEOUT
import com.github.gtache.lsp.requests.Timeout.DOC_HIGHLIGHT_TIMEOUT
import com.github.gtache.lsp.requests.Timeout.EXECUTE_COMMAND_TIMEOUT
import com.github.gtache.lsp.requests.Timeout.FORMATTING_TIMEOUT
import com.github.gtache.lsp.requests.Timeout.HOVER_TIMEOUT
import com.github.gtache.lsp.requests.Timeout.PREPARE_RENAME_TIMEOUT
import com.github.gtache.lsp.requests.Timeout.REFERENCES_TIMEOUT
import com.github.gtache.lsp.requests.Timeout.SIGNATURE_TIMEOUT
import com.github.gtache.lsp.requests.Timeout.WILLSAVE_TIMEOUT
import com.github.gtache.lsp.requests.Timeouts
import com.github.gtache.lsp.requests.WorkspaceEditHandler
import com.github.gtache.lsp.settings.LSPState
import com.github.gtache.lsp.tail
import com.github.gtache.lsp.utils.ApplicationUtils.computableReadAction
import com.github.gtache.lsp.utils.ApplicationUtils.computableWriteAction
import com.github.gtache.lsp.utils.ApplicationUtils.invokeLater
import com.github.gtache.lsp.utils.ApplicationUtils.pool
import com.github.gtache.lsp.utils.ApplicationUtils.writeAction
import com.github.gtache.lsp.utils.DocumentUtils
import com.github.gtache.lsp.utils.DocumentUtils.LSPPosToOffset
import com.github.gtache.lsp.utils.DocumentUtils.LSPRangeToTextRange
import com.github.gtache.lsp.utils.DocumentUtils.expandOffsetToToken
import com.github.gtache.lsp.utils.DocumentUtils.getTextClamped
import com.github.gtache.lsp.utils.DocumentUtils.offsetToLSPPos
import com.github.gtache.lsp.utils.FileUtils
import com.github.gtache.lsp.utils.GUIUtils
import com.github.gtache.lsp.utils.GUIUtils.createAndShowEditorHint
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.lang.LanguageDocumentation
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.ui.Hint
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.uiDesigner.core.Spacer
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.JsonRpcException
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.util.SemanticHighlightingTokens
import java.awt.*
import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.concurrent.timerTask

/**
 * Class handling events related to an Editor (a Document)
 *
 * @param editor              The "watched" editor
 * @param mouseListener       A listener for mouse clicks
 * @param mouseMotionListener A listener for mouse movement
 * @param documentListener    A listener for keystrokes
 * @param selectionListener   A listener for selection changes in the editor
 * @param requestManager      The related RequestManager, connected to the right LanguageServer
 * @param serverOptions       the options of the server regarding completion, signatureHelp, syncKind, etc
 * @param wrapper             The corresponding LanguageServerWrapper
 */
class EditorEventManager(
    val editor: Editor, val mouseListener: EditorMouseListener, val mouseMotionListener: EditorMouseMotionListener,
    val documentListener: DocumentListener, val selectionListener: SelectionListener,
    val requestManager: RequestManager, val serverOptions: ServerOptions, val wrapper: LanguageServerWrapperImpl
) {

    companion object {
        private val logger: Logger = Logger.getInstance(EditorEventManager::class.java)
        private const val PREPARE_DOC_THRES = 10 //Time between requests when ctrl is pressed (10ms)
        private val SHOW_DOC_THRES: Long = EditorSettingsExternalizable.getInstance().tooltipsDelay - PREPARE_DOC_THRES.toLong()

        private val uriToManager: MutableMap<String, EditorEventManager> = HashMap()
        private val editorToManager: MutableMap<Editor, EditorEventManager> = HashMap()

        @Volatile
        private var isKeyPressed = false

        @Volatile
        private var isCtrlDown = false

        @Volatile
        private var docRange: RangeMarker? = null

        init {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { e ->
                synchronized(this) {
                    when (e.id) {
                        KeyEvent.KEY_PRESSED -> {
                            isKeyPressed = true
                            if (e.keyCode == KeyEvent.VK_CONTROL) isCtrlDown = true
                        }
                        KeyEvent.KEY_RELEASED -> {
                            isKeyPressed = false
                            if (e.keyCode == KeyEvent.VK_CONTROL) {
                                isCtrlDown = false
                                editorToManager.keys.forEach { e -> e.contentComponent.cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR) }
                                docRange?.dispose()
                                docRange = null
                            }
                        }
                        else -> Unit
                    }
                    false
                }
            }
        }

        /**
         * @param uri A file uri
         * @return The manager for the given uri, or None
         */
        fun forUri(uri: String): EditorEventManager? {
            prune()
            return uriToManager[uri]
        }

        private fun prune(): Unit {
            editorToManager.filter { e -> !e.value.wrapper.isActive() }.keys.forEach { editorToManager.remove(it) }
            uriToManager.filter { e -> !e.value.wrapper.isActive() }.keys.forEach { uriToManager.remove(it) }
        }

        /**
         * @param editor An editor
         * @return The manager for the given editor, or None
         */
        fun forEditor(editor: Editor): EditorEventManager? {
            prune()
            return editorToManager[editor]
        }

        /**
         * Tells the server that all the documents will be saved
         */
        fun willSaveAll(): Unit {
            prune()
            editorToManager.forEach { e -> e.value.willSave() }
        }
    }

    private val identifier: TextDocumentIdentifier = TextDocumentIdentifier(FileUtils.editorToURIString(editor))
    private val changesParams = DidChangeTextDocumentParams(VersionedTextDocumentIdentifier(), ArrayList())
    private val selectedSymbHighlights: MutableSet<RangeHighlighter> = HashSet()
    private val diagnosticsHighlights: MutableSet<DiagnosticRangeHighlighter> = HashSet()
    private val semanticHighlights: MutableMap<Int, List<RangeHighlighter>> = HashMap()
    private val syncKind = serverOptions.syncKind

    private val completionTriggers = if (serverOptions.completionOptions !== DummyServerOptions.RENAME && serverOptions.completionOptions.triggerCharacters != null)
        serverOptions.completionOptions.triggerCharacters.toSet().filter { s -> s != "." } else emptySet()

    private val signatureTriggers = if (serverOptions.signatureHelpOptions !== DummyServerOptions.SIGNATURE_HELP && serverOptions.signatureHelpOptions.triggerCharacters != null)
        serverOptions.signatureHelpOptions.triggerCharacters.toSet() else emptySet()

    private val documentOnTypeFormattingOptions = serverOptions.documentOnTypeFormattingOptions
    private val onTypeFormattingTriggers = if (documentOnTypeFormattingOptions !== DummyServerOptions.DOCUMENT_ON_TYPE_FORMATTING && documentOnTypeFormattingOptions.moreTriggerCharacter != null)
        (documentOnTypeFormattingOptions.moreTriggerCharacter + documentOnTypeFormattingOptions.firstTriggerCharacter).toSet()
    else if (documentOnTypeFormattingOptions !== DummyServerOptions.DOCUMENT_ON_TYPE_FORMATTING)
        setOf(documentOnTypeFormattingOptions.firstTriggerCharacter)
    else
        emptySet()

    private val semanticHighlightingScopes: List<List<String>> = if (serverOptions.semanticHighlightingOptions !== DummyServerOptions.SEMANTIC_HIGHLIGHTING && serverOptions.semanticHighlightingOptions.scopes != null)
        serverOptions.semanticHighlightingOptions.scopes.toList().map { l -> l.toList() } else emptyList()

    private val project: Project = editor.project!!

    @Volatile
    var needSave = false
    private var showDocThread = Timer("ShowDocThread", true)
    private var prepareDocThread = Timer("PrepareDocThread", true)
    private var showDocTask: TimerTask? = null
    private var prepareDocTask: TimerTask? = null
    private var version: Int = 0
    private var isOpen: Boolean = false
    private var mouseInEditor: Boolean = true
    private var currentHint: Hint? = null
    private var currentDoc: String? = null
    private var holdDCE: Boolean = false
    private val DCEs: MutableList<DocumentEvent> = ArrayList()

    init {
        FileUtils.editorToURIString(editor)?.let { uriToManager[it] = this } ?: logger.warn("Null URI for $editor")
        editorToManager[editor] = this
        changesParams.textDocument.uri = identifier.uri
    }

    /**
     * Calls onTypeFormatting or signatureHelp if the character typed was a trigger character
     *
     * @param c The character just typed
     */
    fun characterTyped(c: Char): Unit {
        if (completionTriggers.contains(c.toString())) {
            completion(offsetToLSPPos(editor, editor.caretModel.currentCaret.offset))
        } else if (signatureTriggers.contains(c.toString())) {
            signatureHelp()
        } else if (onTypeFormattingTriggers.contains(c.toString())) {
            onTypeFormatting(c.toString())
        }
    }

    /**
     * Calls signatureHelp at the current editor caret position
     */
    fun signatureHelp(): Unit {
        val lPos = editor.caretModel.currentCaret.logicalPosition
        val point = editor.logicalPositionToXY(lPos)
        val params = TextDocumentPositionParams(identifier, DocumentUtils.logicalToLSPPos(lPos, editor))
        pool {
            if (!editor.isDisposed) {
                val future = requestManager.signatureHelp(params)
                if (future != null) {
                    try {
                        val signature = future.get(SIGNATURE_TIMEOUT(), TimeUnit.MILLISECONDS)
                        wrapper.notifySuccess(Timeouts.SIGNATURE)
                        if (signature != null) {
                            val signatures = signature.signatures
                            if (signatures != null && signatures.isNotEmpty()) {
                                val activeSignatureIndex = signature.activeSignature
                                val activeParameterIndex = signature.activeParameter
                                val activeParameterLabel = signatures[activeSignatureIndex].parameters[activeParameterIndex].label
                                val activeParameter = if (activeParameterLabel.isLeft) activeParameterLabel.left else
                                    signatures[activeSignatureIndex].label.substring(
                                        activeParameterLabel.right.first,
                                        activeParameterLabel.right.second
                                    )
                                val builder = StringBuilder()
                                builder.append("<html>")
                                signatures.take(activeSignatureIndex).forEach { sig -> builder.append(sig.label).append("<br>") }
                                builder.append("<b>").append(
                                    signatures[activeSignatureIndex].label
                                        .replace(activeParameter, "<font color=\"yellow\">$activeParameter</font>")
                                ).append("</b>")
                                signatures.drop(activeSignatureIndex + 1).forEach { sig -> builder.append("<br>").append(sig.label) }
                                builder.append("</html>")
                                val flags = HintManager.HIDE_BY_ESCAPE or HintManager.HIDE_BY_OTHER_HINT or HintManager.HIDE_IF_OUT_OF_EDITOR
                                invokeLater { currentHint = createAndShowEditorHint(editor, builder.toString(), point, HintManager.UNDER, flags = flags) }
                            }
                        }
                    } catch (e: TimeoutException) {
                        logger.warn(e)
                        wrapper.notifyFailure(Timeouts.SIGNATURE)
                    } catch (e: Exception) {
                        e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                            logger.warn(e)
                            wrapper.crashed(e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves the commands needed to apply a CodeAction
     *
     * @param element The element which needs the CodeAction
     * @return The list of commands, or null if none are given / the request times out
     */
    fun codeAction(element: LSPPsiElement): Iterable<Either<Command, CodeAction>>? {
        val params = CodeActionParams()
        params.textDocument = identifier
        val range = Range(offsetToLSPPos(editor, element.start), offsetToLSPPos(editor, element.end))
        params.range = range
        val context = CodeActionContext(diagnosticsHighlights.map { it.diagnostic })
        params.context = context
        val future = requestManager.codeAction(params)
        return if (future != null) {
            try {
                val res = future.get(CODEACTION_TIMEOUT(), TimeUnit.MILLISECONDS)
                wrapper.notifySuccess(Timeouts.CODEACTION)
                res
            } catch (e: TimeoutException) {
                logger.warn(e)
                wrapper.notifyFailure(Timeouts.CODEACTION)
                null
            } catch (e: Exception) {
                e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                    logger.warn(e)
                    wrapper.crashed(e)
                    null
                }
            }
        } else {
            null
        }
    }

    data class TemplateInfo(val start: Int, val end: Int, val num: String, val placeholder: String)

    /**
     * Returns the completion suggestions given a position
     *
     * @param pos The LSP position
     * @return The suggestions
     */
    fun completion(pos: Position): Iterable<LookupElement> {
        val request = requestManager.completion(CompletionParams(identifier, pos))
        return if (request != null) {
            try {
                val res = request.get(COMPLETION_TIMEOUT(), TimeUnit.MILLISECONDS)
                wrapper.notifySuccess(Timeouts.COMPLETION)
                if (res != null) {
                    val completion /*: CompletionList | List<CompletionItem> */ = if (res.isLeft) res.left else res.right

                    /**
                     * Creates a LookupElement given a CompletionItem
                     *
                     * @param item The CompletionItem
                     * @return The corresponding LookupElement
                     */
                    fun createLookupItem(item: CompletionItem): LookupElement {
                        fun execCommand(command: Command): Unit {
                            executeCommands(listOf(command))
                        }

                        fun prepareTemplate(insertText: String): Template {
                            val startIndexes = 0.until(insertText.length).filter { insertText.startsWith("$", it) }
                            val variables = startIndexes.map { i ->
                                val sub = insertText.drop(i + 1)
                                if (sub.head == '{') {
                                    val num = sub.tail.takeWhile { c -> c != ':' }
                                    val placeholder = sub.tail.dropWhile { c -> c != ':' }.tail.takeWhile { c -> c != '}' }
                                    val len = num.length + placeholder.length + 4
                                    TemplateInfo(i, i + len, num, placeholder)
                                } else {
                                    val num = sub.takeWhile { c -> c.isDigit() }
                                    val placeholder = "..."
                                    val len = num.length + 1
                                    TemplateInfo(i, i + len, num, placeholder)
                                }
                            }
                            var newInsertText = insertText
                            variables.sortedBy { t -> -t.start }
                                .forEach { t -> newInsertText = newInsertText.take(t.start) + "$" + t.num + "$" + newInsertText.drop(t.end) }

                            fun getRandomString(length: Int): String {
                                val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
                                return (1..length)
                                    .map { allowedChars.random() }
                                    .joinToString("")
                            }

                            val template = TemplateManager.getInstance(project)
                                .createTemplate("anon" + getRandomString(5), "lsp")

                            variables.forEach { t ->
                                template.addVariable(t.num, TextExpression(t.placeholder), TextExpression(t.placeholder), true, false)
                            }
                            template.setInline(true)
                            (template as TemplateImpl).string = newInsertText
                            return template
                        }


                        //TODO improve
                        val addTextEdits = item.additionalTextEdits
                        val command = item.command
                        val commitChars = item.commitCharacters
                        val data = item.data
                        val deprecated = item.deprecated
                        val detail = item.detail
                        val doc = item.documentation
                        val filterText = item.filterText
                        val insertText = item.insertText
                        val insertFormat = item.insertTextFormat //TODO snippet
                        val kind = item.kind
                        val label = item.label
                        val textEdit = item.textEdit
                        val sortText = item.sortText
                        val presentableText = if (label != null && label != "") label else if (insertText != null) insertText else ""
                        val tailText = if (detail != null) "\t" + detail else ""
                        val iconProvider = GUIUtils.getIconProviderFor(wrapper.serverDefinition)
                        val icon = iconProvider.getCompletionIcon(kind)
                        var lookupElementBuilder: LookupElementBuilder? = null
                        /*            .,Renderer((element: LookupElement, presentation: LookupElementPresentation) -> { //TODO later
                                  presentation when {
                                    realPresentation: RealLookupElementPresentation ->
                                      if (!realPresentation.hasEnoughSpaceFor(presentation.getItemText, presentation.isItemTextBold)) {
                                      }
                                  }
                                })*/

                        fun runSnippet(template: Template): Unit {
                            invokeLater {
                                writeAction {
                                    CommandProcessor.getInstance().executeCommand(
                                        project,
                                        { editor.document.insertString(editor.caretModel.offset, template.templateText) },
                                        "snippetInsert",
                                        "lsp",
                                        editor.document
                                    )
                                }
                                TemplateManager.getInstance(project).startTemplate(editor, template)
                                if (addTextEdits != null) {
                                    applyEdit(edits = addTextEdits, name = "Additional Completions : $label")
                                }
                                execCommand(command)
                            }
                        }

                        fun applyEdits(edit: List<TextEdit>, moveToCaret: Boolean): Unit {
                            invokeLater {
                                if (edit.isNotEmpty()) {
                                    applyEdit(edits = edit, name = "Completion : $label")
                                }
                                execCommand(command)
                                if (moveToCaret) {
                                    editor.caretModel.moveCaretRelatively(textEdit.newText.length, 0, false, false, true)
                                }
                            }
                        }

                        if (textEdit != null) {
                            if (addTextEdits != null) {
                                lookupElementBuilder = LookupElementBuilder.create(presentableText, "").withInsertHandler { context, _ ->
                                    context.commitDocument()
                                    if (insertFormat == InsertTextFormat.Snippet) {
                                        val template = prepareTemplate(textEdit.newText)
                                        runSnippet(template)
                                    } else {
                                        applyEdits(listOf(textEdit) + addTextEdits, moveToCaret = true)
                                    }
                                }.withLookupString(presentableText)
                            } else {
                                lookupElementBuilder = LookupElementBuilder.create(presentableText, "")
                                    .withInsertHandler { context, _ ->
                                        context.commitDocument()
                                        if (insertFormat == InsertTextFormat.Snippet) {
                                            val template = prepareTemplate(textEdit.newText)
                                            runSnippet(template)
                                        } else {
                                            applyEdits(listOf(textEdit), moveToCaret = true)
                                        }
                                    }.withLookupString(presentableText)
                            }
                        } else if (addTextEdits != null) {
                            lookupElementBuilder = LookupElementBuilder.create(presentableText, "")
                                .withInsertHandler { context, _ ->
                                    context.commitDocument()
                                    if (insertFormat == InsertTextFormat.Snippet) {
                                        val template = prepareTemplate(if (insertText != null && insertText != "") insertText else label)
                                        runSnippet(template)
                                    } else {
                                        applyEdits(addTextEdits, moveToCaret = false)
                                    }
                                }.withLookupString(presentableText)
                        } else {
                            lookupElementBuilder = LookupElementBuilder.create(if (insertText != null && insertText != "") insertText else label)
                            if (command != null) lookupElementBuilder = lookupElementBuilder.withInsertHandler { context, _ ->
                                context.commitDocument()
                                if (insertFormat == InsertTextFormat.Snippet) {
                                    val template = prepareTemplate(if (insertText != null && insertText != "") insertText else label)
                                    runSnippet(template)
                                }
                                applyEdits(emptyList(), moveToCaret = false)
                            }
                        }
                        if (kind == CompletionItemKind.Keyword) lookupElementBuilder = lookupElementBuilder.withBoldness(true)
                        if (deprecated) {
                            lookupElementBuilder = lookupElementBuilder.withStrikeoutness(true)
                        }
                        return lookupElementBuilder.withPresentableText(presentableText).withTailText(tailText, true).withIcon(icon)
                            .withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT)
                    }

                    when (completion) {
                        is CompletionList -> completion.items.map { item -> createLookupItem(item) }
                        is Iterable<*> -> completion.map { item -> createLookupItem(item as CompletionItem) }
                        else -> {
                            logger.warn("Unknown completion type : $completion")
                            emptyList()
                        }
                    }
                } else emptyList()
            } catch (e: TimeoutException) {
                logger.warn(e)
                wrapper.notifyFailure(Timeouts.COMPLETION)
                emptyList()
            } catch (e: Exception) {
                e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                    logger.warn(e)
                    wrapper.crashed(e)
                    emptyList()
                }
            }
        } else emptyList()
    }

    /**
     * Sends commands to execute to the server and applies the changes returned if the future returns a WorkspaceEdit
     *
     * @param commands The commands to execute
     */
    fun executeCommands(commands: Iterable<Command>): Unit {
        pool {
            if (!editor.isDisposed) {
                commands.map { c ->
                    requestManager.executeCommand(ExecuteCommandParams(c.command, c.arguments))
                }.forEach { f ->
                    if (f != null) {
                        try {
                            val ret = f.get(EXECUTE_COMMAND_TIMEOUT(), TimeUnit.MILLISECONDS)
                            wrapper.notifySuccess(Timeouts.EXECUTE_COMMAND)
                            when (ret) {
                                is WorkspaceEdit -> WorkspaceEditHandler.applyEdit(ret, name = "Execute command")
                                else -> logger.warn("ExecuteCommand returned $ret")
                            }
                        } catch (e: TimeoutException) {
                            logger.warn(e)
                            wrapper.notifyFailure(Timeouts.EXECUTE_COMMAND)
                        } catch (e: Exception) {
                            e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                                logger.warn(e)
                                wrapper.crashed(e)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Applies the diagnostics to the document
     *
     * @param diagnostics The diagnostics to apply from the server
     */
    fun diagnostics(diagnostics: Iterable<Diagnostic>): Unit {
        fun rangeToOffsets(range: Range): TextRange {
            val (start, end) = Pair(LSPPosToOffset(editor, range.start), LSPPosToOffset(editor, range.end))
            return if (start == end) {
                expandOffsetToToken(editor, start)
            } else {
                TextRange(start, end)
            }
        }

        invokeLater {
            if (!editor.isDisposed) {
                synchronized(diagnosticsHighlights) {
                    diagnosticsHighlights.forEach { highlight -> editor.markupModel.removeHighlighter(highlight.rangeHighlighter) }
                    diagnosticsHighlights.clear()
                }
                for (diagnostic in diagnostics) {
                    val range = diagnostic.range
                    val severity = diagnostic.severity

                    val markupModel = editor.markupModel
                    val colorScheme = editor.colorsScheme

                    val (effectType, effectColor, layer) = when (severity) {
                        null -> Triple(null, null, null)
                        DiagnosticSeverity.Error -> Triple(
                            EffectType.WAVE_UNDERSCORE, Color.RED, HighlighterLayer.ERROR
                        )
                        DiagnosticSeverity.Warning
                        -> Triple(
                            EffectType.WAVE_UNDERSCORE, Color.YELLOW, HighlighterLayer.WARNING
                        )
                        DiagnosticSeverity.Information
                        -> Triple(
                            EffectType.WAVE_UNDERSCORE, Color.GRAY, HighlighterLayer.WARNING
                        )
                        DiagnosticSeverity.Hint
                        -> Triple(
                            EffectType.BOLD_DOTTED_LINE, Color.GRAY, HighlighterLayer.WARNING
                        )
                    }

                    val textRange = rangeToOffsets(range)
                    val start = textRange.startOffset
                    val end = textRange.endOffset
                    layer?.let {
                        synchronized(diagnosticsHighlights) {
                            diagnosticsHighlights
                                .add(
                                    DiagnosticRangeHighlighter(
                                        markupModel.addRangeHighlighter(
                                            start,
                                            end,
                                            it,
                                            TextAttributes(colorScheme.defaultForeground, colorScheme.defaultBackground, effectColor, effectType, Font.PLAIN),
                                            HighlighterTargetArea.EXACT_RANGE
                                        ),
                                        diagnostic
                                    )
                                )
                        }
                    }
                }
            }
        }
    }

    private fun releaseDCE(): Unit {
        synchronized(DCEs) {
            if (!editor.isDisposed) {
                synchronized(changesParams) {
                    val changeEvent = TextDocumentContentChangeEvent()
                    when (syncKind) {
                        TextDocumentSyncKind.None -> Unit
                        TextDocumentSyncKind.Full -> {
                            changeEvent.text = (editor.document.text)
                            changesParams.contentChanges.add(changeEvent)
                        }
                        TextDocumentSyncKind.Incremental ->
                            DCEs.filter { e -> e.document == editor.document }.map { event ->
                                val newText = event.newFragment
                                val offset = event.offset
                                val newTextLength = event.newLength
                                val lspPosition: Position = offsetToLSPPos(editor, offset)
                                val startLine = lspPosition.line
                                val startColumn = lspPosition.character
                                val oldText = event.oldFragment

                                //if text was deleted/replaced, calculate the end position of inserted/deleted text
                                val (endLine, endColumn) = if (oldText.length > 0) {
                                    val line = startLine + StringUtil.countNewLines(oldText)
                                    val oldLines = oldText.toString().split('\n')
                                    val oldTextLength = if (oldLines.isEmpty()) 0 else oldLines.last().length
                                    val column = if (oldLines.size == 1) startColumn + oldTextLength else oldTextLength
                                    Pair(line, column)
                                } else Pair(startLine, startColumn) //if insert or no text change, the end position is the same
                                val range = Range(Position(startLine, startColumn), Position(endLine, endColumn))
                                changeEvent.range = range
                                changeEvent.rangeLength = newTextLength
                                changeEvent.text = newText.toString()
                                changeEvent
                            }.forEach { changesParams.contentChanges.add(it) }
                    }
                    requestManager.didChange(changesParams)
                    changesParams.contentChanges.clear()
                }
            }
        }
    }

    private fun cancelDoc(): Unit {
        try {
            currentHint?.hide()
            currentHint = null
            prepareDocTask?.cancel()
            showDocTask?.cancel()
            docRange?.dispose()
            docRange = null
        } catch (e: Exception) {
            logger.warn(e)
        }
    }

    /**
     * Handles the DocumentChanged events
     *
     * @param event The DocumentEvent
     */
    fun documentChanged(event: DocumentEvent): Unit {
        if (holdDCE) {
            synchronized(DCEs) {
                DCEs.add(event)
            }
        } else {
            if (!editor.isDisposed) {
                if (event.document == editor.document) {
                    cancelDoc()
                    synchronized(changesParams) {
                        changesParams.textDocument.version = ++version
                        val changeEvent = TextDocumentContentChangeEvent()
                        when (syncKind) {
                            TextDocumentSyncKind.None -> Unit
                            TextDocumentSyncKind.Incremental -> {
                                val newText = event.newFragment
                                val offset = event.offset
                                val newTextLength = event.newLength
                                val lspPosition = offsetToLSPPos(editor, offset)
                                val startLine = lspPosition.line
                                val startColumn = lspPosition.character
                                val oldText = event.oldFragment.toString()
                                //if text was deleted/replaced, calculate the end position of inserted/deleted text
                                val (endLine, endColumn) = if (oldText.length > 0) {
                                    val line = startLine + StringUtil.countNewLines(oldText)
                                    val oldLines = oldText.split('\n')
                                    val oldTextLength = if (oldLines.isEmpty()) 0 else oldLines.last().length
                                    val column = if (oldText.endsWith("\n")) 0 else if (oldLines.size == 1) startColumn + oldTextLength else oldTextLength
                                    Pair(line, column)
                                } else Pair(startLine, startColumn) //if insert or no text change, the end position is the same
                                val range = Range(Position(startLine, startColumn), Position(endLine, endColumn))
                                changeEvent.range = (range)
                                changeEvent.rangeLength = LSPPosToOffset(editor, range.end) - LSPPosToOffset(editor, range.start)
                                changeEvent.text = newText.toString()
                                changesParams.contentChanges.add(changeEvent)
                            }
                            TextDocumentSyncKind.Full -> {
                                changeEvent.text = editor.document.text
                                changesParams.contentChanges.add(changeEvent)
                            }
                        }
                        requestManager.didChange(changesParams)
                        changesParams.contentChanges.clear()
                    }
                } else {
                    logger.error("Wrong document for the EditorEventManager")
                }
            }
        }
    }

    /**
     * Notifies the server that the corresponding document has been closed
     */
    fun documentClosed(): Unit {
        pool {
            if (isOpen) {
                requestManager.didClose(DidCloseTextDocumentParams(identifier))
                isOpen = false
                editorToManager.remove(editor)
                uriToManager.remove(FileUtils.editorToURIString(editor))
            } else {
                logger.warn("Editor " + identifier.uri + " was already closed")
            }
        }
    }

    fun documentOpened(): Unit {
        pool {
            if (!editor.isDisposed) {
                if (isOpen) {
                    logger.warn("Editor $editor was already open")
                } else {
                    requestManager.didOpen(
                        DidOpenTextDocumentParams(
                            TextDocumentItem(
                                identifier.uri,
                                wrapper.serverDefinition.id,
                                version,
                                editor.document.text
                            )
                        )
                    )
                    isOpen = true
                }
            }
        }
    }

    /**
     * Notifies the server that the corresponding document has been saved
     */
    fun documentSaved(): Unit {
        pool {
            if (!editor.isDisposed) {
                val params = DidSaveTextDocumentParams(identifier, editor.document.text)
                requestManager.didSave(params)
            }
        }
    }

    /**
     * Indicates that the document will be saved
     */
    //TODO Manual
    fun willSave(): Unit {
        if (wrapper.isWillSaveWaitUntil() && !needSave) willSaveWaitUntil() else pool {
            if (!editor.isDisposed) requestManager.willSave(WillSaveTextDocumentParams(identifier, TextDocumentSaveReason.Manual))
        }
    }

    /**
     * If the server supports willSaveWaitUntil, the LSPVetoer will check if  a save is needed
     * (needSave will basically alterate between true or false, so the document will always be saved)
     */
    private fun willSaveWaitUntil(): Unit {
        if (wrapper.isWillSaveWaitUntil()) {
            pool {
                if (!editor.isDisposed) {
                    val params = WillSaveTextDocumentParams(identifier, TextDocumentSaveReason.Manual)
                    val future = requestManager.willSaveWaitUntil(params)
                    if (future != null) {
                        try {
                            val edits = future.get(WILLSAVE_TIMEOUT(), TimeUnit.MILLISECONDS)
                            wrapper.notifySuccess(Timeouts.WILLSAVE)
                            if (edits != null) {
                                invokeLater { applyEdit(edits = edits, name = "WaitUntil edits") }
                            }
                        } catch (e: TimeoutException) {
                            logger.warn(e)
                            wrapper.notifyFailure(Timeouts.WILLSAVE)
                        } catch (e: Exception) {
                            e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                                logger.warn(e)
                                wrapper.crashed(e)
                            }
                        } finally {
                            needSave = true
                            saveDocument()
                        }
                    } else {
                        needSave = true
                        saveDocument()
                    }
                }
            }
        } else {
            logger.error("Server doesn't support WillSaveWaitUntil")
            needSave = true
            saveDocument()
        }
    }

    /**
     * Gets references, synchronously
     *
     * @param offset The offset of the element
     * @return A list of start/end offset
     */
    fun documentReferences(offset: Int): Iterable<Pair<Int, Int>>? {
        val params = ReferenceParams()
        val context = ReferenceContext()
        context.isIncludeDeclaration = true
        params.context = (context)
        params.position = (offsetToLSPPos(editor, offset))
        params.textDocument = (identifier)
        val future = requestManager.references(params)
        return if (future != null) {
            try {
                val references = future.get(REFERENCES_TIMEOUT(), TimeUnit.MILLISECONDS)
                wrapper.notifySuccess(Timeouts.REFERENCES)
                references?.filterIsInstance<Location>()?.filter { FileUtils.sanitizeURI(it.uri) == identifier.uri }
                    ?.map { Pair(LSPPosToOffset(editor, it.range.start), LSPPosToOffset(editor, it.range.end)) }
            } catch (e: TimeoutException) {
                logger.warn(e)
                wrapper.notifyFailure(Timeouts.REFERENCES)
                null
            } catch (e: Exception) {
                e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                    logger.warn(e)
                    wrapper.crashed(e)
                    null
                }
            }
        } else {
            null
        }
    }

    /**
     * @return The current diagnostics highlights
     */
    fun getDiagnostics(): Set<DiagnosticRangeHighlighter> {
        return diagnosticsHighlights.toSet()
    }

    fun getElementAtOffset(offset: Int): LSPPsiElement? {
        return computableReadAction(Computable {
            if (!editor.isDisposed) {
                val params = TextDocumentPositionParams(identifier, offsetToLSPPos(editor, offset))
                val future = requestManager.documentHighlight(params)
                if (future != null) {
                    try {
                        val res = future.get(DOC_HIGHLIGHT_TIMEOUT(), TimeUnit.MILLISECONDS)
                        wrapper.notifySuccess(Timeouts.DOC_HIGHLIGHT)
                        if (res != null && !editor.isDisposed) {
                            val range = res.map { dh ->
                                TextRange(
                                    LSPPosToOffset(editor, dh.range.start),
                                    LSPPosToOffset(editor, dh.range.end)
                                )
                            }.find { range -> range.startOffset <= offset && offset <= range.endOffset }
                            if (range != null) {
                                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
                                if (psiFile != null) {
                                    LSPPsiElement(
                                        editor.document.getText(range),
                                        project,
                                        range.startOffset,
                                        range.endOffset,
                                        psiFile,
                                        editor
                                    )
                                } else null
                            } else {
                                null
                            }
                        } else null
                    } catch (e: TimeoutException) {
                        logger.warn(e)
                        wrapper.notifyFailure(Timeouts.DOC_HIGHLIGHT)
                        null
                    } catch (e: Exception) {
                        e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                            logger.warn(e)
                            wrapper.crashed(e)
                            null
                        }
                    }
                } else null
            } else null
        })
    }

    /**
     * Called when the mouse is clicked
     * At the moment, is used by CTRL+click to see references / goto definition
     *
     * @param e The mouse event
     */
    fun mouseClicked(e: EditorMouseEvent): Unit {
        if (e.mouseEvent.isControlDown && docRange != null && docRange!!.loc != null && docRange!!.loc!!.targetUri != null) {
            val loc = docRange!!.loc!!
            val offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(e.mouseEvent.point))
            val locUri = FileUtils.sanitizeURI(loc.targetUri)
            if (identifier.uri == locUri) {
                if (docRange!!.definitionContainsOffset(offset)) {
                    (ActionManager.getInstance().getAction("LSPFindUsages") as LSPReferencesAction).forManagerAndOffset(this, offset)
                } else {
                    val startOffset = LSPPosToOffset(editor, loc.targetRange.start)
                    writeAction {
                        editor.caretModel.moveToOffset(startOffset)
                        editor.selectionModel.setSelection(startOffset, LSPPosToOffset(editor, loc.targetRange.end))
                        editor.scrollingModel.scrollToCaret(ScrollType.CENTER);
                    }
                }
            } else {
                val file = LocalFileSystem.getInstance().findFileByIoFile(File(URI(locUri)))
                if (file != null) {
                    val descriptor = OpenFileDescriptor(project, file)
                    writeAction {
                        val newEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
                        if (newEditor != null) {
                            val startOffset = LSPPosToOffset(newEditor, loc.targetRange.start)
                            newEditor.caretModel.currentCaret.moveToOffset(startOffset)
                            newEditor.selectionModel.setSelection(startOffset, LSPPosToOffset(newEditor, loc.targetRange.end))
                            newEditor.scrollingModel.scrollToCaret(ScrollType.CENTER);
                        }
                    }
                } else {
                    logger.warn("Empty file for $locUri")
                }
            }
            cancelDoc()
        }
    }

    /**
     * Queries references and show them in a window, given the offset of the symbol in the editor
     *
     * @param offset The offset
     */
    fun showReferences(offset: Int): Unit {
        invokeLater {
            if (!editor.isDisposed) {
                writeAction { editor.caretModel.currentCaret.moveToOffset(offset) }
                showReferences(includeDefinition = false)
            }
        }
    }

    /**
     * Queries references and show a window , these references (click on a row to get to the location)
     */
    fun showReferences(includeDefinition: Boolean = true): Unit {
        pool {
            if (!editor.isDisposed) {
                val context = ReferenceContext(includeDefinition)
                val params = ReferenceParams(context)
                params.textDocument = (identifier)
                val serverPos = computableReadAction {
                    DocumentUtils.logicalToLSPPos(editor.caretModel.currentCaret.logicalPosition, editor)
                }
                params.position = (serverPos)
                val future = requestManager.references(params)
                if (future != null) {
                    try {
                        val references = future.get(REFERENCES_TIMEOUT(), TimeUnit.MILLISECONDS)
                        wrapper.notifySuccess(Timeouts.REFERENCES)
                        if (references != null) {
                            invokeLater {
                                if (!editor.isDisposed) showReferences(references)
                            }
                        }
                    } catch (e: TimeoutException) {
                        logger.warn(e)
                        wrapper.notifyFailure(Timeouts.REFERENCES)
                    } catch (e: Exception) {
                        e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                            logger.warn(e)
                            wrapper.crashed(e)
                        }
                    }
                }
            }
        }
    }

    data class TextLocation(val uri: String, val startOffset: Int, val endOffset: Int, val extract: String)
    data class EditorLocation(val startOffset: Int, val endOffset: Int, val name: String, val extract: String)

    private fun showReferences(references: Iterable<Location>): Unit {
        var name = ""

        /**
         * Opens the editor and get the required infos
         *
         * @param file              The file of the editor
         * @param fileEditorManager The fileEditorManager
         * @param start             The starting position
         * @param end               The ending position
         * @return (StartOffset, EndOffset, Name (of the symbol), line (containing the symbol))
         */
        fun openEditorAndGetOffsetsAndName(
            file: VirtualFile,
            fileEditorManager: FileEditorManager,
            start: Position,
            end: Position
        ): EditorLocation {
            val descriptor = OpenFileDescriptor(project, file)
            return computableWriteAction(Computable {
                val newEditor = fileEditorManager.openTextEditor(descriptor, false)
                if (newEditor != null) {
                    val startOffset = LSPPosToOffset(newEditor, start)
                    val endOffset = LSPPosToOffset(newEditor, end)
                    val doc = newEditor.document
                    val docText = doc.getTextClamped(TextRange(startOffset, endOffset))
                    fileEditorManager.closeFile(file)
                    EditorLocation(startOffset, endOffset, docText, DocumentUtils.getLineText(newEditor, startOffset, endOffset))
                } else EditorLocation(-1, -1, "", "")
            })
        }

        val locations = references.mapNotNull { l ->
            val start = l.range.start
            val end = l.range.end
            val uri = FileUtils.sanitizeURI(l.uri)
            var startOffset = -1
            var endOffset = -1
            var sample = ""

            /**
             * Opens the editor to retrieve the offsets, line, etc if needed
             */
            fun manageUnopenedEditor(): Unit {
                val file = LocalFileSystem.getInstance().findFileByIoFile(File(URI(uri)))
                val fileEditorManager = FileEditorManager.getInstance(project)
                if (file != null) {
                    if (fileEditorManager.isFileOpen(file)) {
                        val editors = fileEditorManager.getAllEditors(file).filterIsInstance<TextEditor>().map { t -> t.editor }
                        if (editors.isEmpty()) {
                            val (s, e, n, sa) = openEditorAndGetOffsetsAndName(file, fileEditorManager, start, end)
                            startOffset = s
                            endOffset = e
                            name = n
                            sample = sa
                        } else {
                            startOffset = LSPPosToOffset(editors.head, start)
                            endOffset = LSPPosToOffset(editors.head, end)
                        }
                    } else {
                        val (s, e, n, sa) = openEditorAndGetOffsetsAndName(file, fileEditorManager, start, end)
                        startOffset = s
                        endOffset = e
                        name = n
                        sample = sa
                    }
                }
            }

            val manager = forUri(uri)
            if (manager != null) {
                try {
                    startOffset = LSPPosToOffset(manager.editor, start)
                    endOffset = LSPPosToOffset(manager.editor, end)
                    name = manager.editor.document.getTextClamped(TextRange(startOffset, endOffset))
                    sample = DocumentUtils.getLineText(manager.editor, startOffset, endOffset)
                } catch (e: RuntimeException) {
                    logger.warn(e)
                    manageUnopenedEditor()
                }
            } else {
                manageUnopenedEditor()
            }
            if (startOffset != -1) {
                TextLocation(uri, startOffset, endOffset, sample)
            } else null
        }.toTypedArray()

        val caretPoint = editor.logicalPositionToXY(editor.caretModel.currentCaret.logicalPosition)
        showReferencesWindow(locations, name, caretPoint)
    }

    /**
     * Creates and shows the references window given the locations
     *
     * @param locations The locations : The file URI, the start offset, end offset, and the sample (line) containing the offsets
     * @param name      The name of the symbol
     * @param point     The point at which to show the window
     */
    private fun showReferencesWindow(locations: Array<TextLocation>, name: String, point: Point): Unit {
        if (locations.isEmpty()) {
            val flags = HintManager.HIDE_BY_ANY_KEY or HintManager.HIDE_BY_CARET_MOVE or HintManager.HIDE_BY_ESCAPE or HintManager.HIDE_BY_LOOKUP_ITEM_CHANGE or
                    HintManager.HIDE_BY_OTHER_HINT or HintManager.HIDE_BY_SCROLLING or HintManager.HIDE_BY_TEXT_CHANGE or HintManager.HIDE_IF_OUT_OF_EDITOR
            invokeLater { if (!editor.isDisposed) currentHint = createAndShowEditorHint(editor, "No usages found", point, flags = flags) }
        } else {
            val frame = JFrame()
            frame.title = "Usages of " + name + " (" + locations.size + (if (locations.size > 1) " usages found)" else " usage found")
            val panel = JPanel()
            var row = 0
            panel.layout = GridLayoutManager(locations.size, 4, Insets(10, 10, 10, 10), -1, -1)
            locations.forEach { l ->
                val listener = object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent): Unit {
                        val file = LocalFileSystem.getInstance().findFileByIoFile(File(URI(FileUtils.sanitizeURI(l.uri))))
                        file?.let {
                            val descriptor = OpenFileDescriptor(project, file, l.startOffset)
                            writeAction {
                                val newEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
                                newEditor?.let {
                                    if (l.startOffset != -1 && l.endOffset != -1) newEditor.selectionModel.setSelection(l.startOffset, l.endOffset)
                                } ?: run {
                                    logger.warn("newEditor is null for $descriptor")
                                }
                            }
                            frame.isVisible = false
                            frame.dispose()
                        } ?: run {
                            logger.warn("file is null for ${l.uri}")
                        }
                    }
                }
                val fileLabel = JLabel(File(URI(FileUtils.sanitizeURI(l.uri))).name)
                val spacer = Spacer()
                val offsetLabel = JLabel(l.startOffset.toString())
                val sampleLabel = JLabel("<html>" + l.extract + "</html>")
                panel.add(
                    fileLabel,
                    GridConstraints(
                        row,
                        0,
                        1,
                        1,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        0,
                        false
                    )
                )
                panel.add(
                    spacer,
                    GridConstraints(
                        row,
                        1,
                        1,
                        1,
                        GridConstraints.ANCHOR_CENTER,
                        GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW,
                        1,
                        null,
                        null,
                        null,
                        0,
                        false
                    )
                )
                panel.add(
                    offsetLabel,
                    GridConstraints(
                        row,
                        2,
                        1,
                        1,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        0,
                        false
                    )
                )
                panel.add(
                    sampleLabel,
                    GridConstraints(
                        row,
                        3,
                        1,
                        1,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        0,
                        false
                    )
                )
                row += 1
                //TODO refine
                fileLabel.addMouseListener(listener)
                spacer.addMouseListener(listener)
                offsetLabel.addMouseListener(listener)
                sampleLabel.addMouseListener(listener)
            }
            panel.isVisible = true
            frame.contentPane = panel

            frame.setLocationRelativeTo(editor.contentComponent)
            frame.location = point
            frame.pack()
            frame.isAutoRequestFocus = true
            frame.isAlwaysOnTop = true
            frame.isVisible = true
        }
    }

    private fun createRange(startOffset: Int, endOffset: Int, getDefinition: Boolean = false, visible: Boolean = true): Unit {
        val loc = if (getDefinition) requestDefinition(offsetToLSPPos(editor, (endOffset + startOffset) / 2)) else null
        val isDefinition = loc != null && LSPPosToOffset(editor, loc.targetRange.start) == startOffset
        invokeLater {
            docRange?.dispose()
            if (!editor.isDisposed) {
                val range = if (!isDefinition && visible) editor.markupModel.addRangeHighlighter(
                    startOffset,
                    endOffset,
                    HighlighterLayer.HYPERLINK,
                    editor.colorsScheme.getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR),
                    HighlighterTargetArea.EXACT_RANGE
                ) else null
                docRange = RangeMarker(startOffset, endOffset, editor, loc, range, isDefinition)
            }
        }
    }

    /**
     * Returns the position of the definition given a position in the editor
     *
     * @param position The position
     * @return The location of the definition
     */
    private fun requestDefinition(position: Position): LocationLink? {
        val params = TextDocumentPositionParams(identifier, position)
        val request = requestManager.definition(params)
        return if (request != null) {
            try {
                val definition = request.get(DEFINITION_TIMEOUT(), TimeUnit.MILLISECONDS)
                wrapper.notifySuccess(Timeouts.DEFINITION)
                if (definition != null) {
                    if (definition.isLeft) {
                        val left = definition.left
                        if (left != null && left.isNotEmpty()) {
                            val loc = left[0]
                            LocationLink(loc.uri, loc.range, loc.range)
                        } else null
                    } else {
                        val right = definition.right
                        if (right != null && right.isNotEmpty()) {
                            val locLink = right[0]
                            locLink
                        } else null
                    }
                } else null
            } catch (e: TimeoutException) {
                logger.warn(e)
                wrapper.notifyFailure(Timeouts.DEFINITION)
                null
            } catch (e: Exception) {
                e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                    logger.warn(e)
                    wrapper.crashed(e)
                    null
                }
            }
        } else {
            null
        }
    }

    /**
     * Will show documentation if the mouse doesn't move for a given time (Hover)
     *
     * @param e the event
     */
    fun mouseMoved(e: EditorMouseEvent): Unit {
        if (e.editor == editor) {
            val ctrlDown = e.mouseEvent.isControlDown
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            if (psiFile != null) {
                val language = psiFile.language
                if ((LSPState.instance?.isAlwaysSendRequests != false || LanguageDocumentation.INSTANCE.allForLanguage(language)
                        .isEmpty() || language == PlainTextLanguage.INSTANCE)
                    && (ctrlDown || EditorSettingsExternalizable.getInstance().isShowQuickDocOnMouseOverElement)) {
                    val lPos = getPos(e)
                    if (lPos != null) {
                        val offset = editor.logicalPositionToOffset(lPos)
                        docRange?.let { range ->
                            if (!range.highlightContainsOffset(offset)) {
                                cancelDoc()
                                scheduleCreateRange(offsetToLSPPos(editor, offset), e.mouseEvent.point, getDefinition = ctrlDown, visible = ctrlDown)
                            } else {
                                if (!ctrlDown) {
                                    currentDoc?.let {
                                        scheduleShowDoc(it, e.mouseEvent.point)
                                        editor.contentComponent.cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
                                    }
                                } else {
                                    editor.contentComponent.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                    if (range.loc == null) {
                                        cancelDoc()
                                        scheduleCreateRange(offsetToLSPPos(editor, offset), e.mouseEvent.point, getDefinition = true, visible = true)
                                    } else Unit
                                }
                            }
                        }
                    }
                }
            } else {
                logger.warn("Null psiFile or LSPState")
            }
        } else {
            logger.error("Wrong editor for EditorEventManager")
        }
    }

    private fun scheduleCreateRange(serverPos: Position, point: Point, getDefinition: Boolean = true, visible: Boolean = true): Unit {
        try {
            prepareDocTask?.cancel()
            prepareDocThread.purge()
            prepareDocTask = timerTask {
                val offset = LSPPosToOffset(editor, serverPos)
                val hover = requestHover(editor, offset)
                if (hover != null) {
                    val doc = HoverHandler.getHoverString(hover)
                    currentDoc = doc
                    val range = if (hover.range == null) rangeForOffset(offset) else LSPRangeToTextRange(editor, hover.range)
                    createRange(range.startOffset, range.endOffset, getDefinition = getDefinition, visible = visible)
                    invokeLater {
                        docRange?.let {
                            scheduleShowDoc(
                                if (it.definitionContainsOffset(offset)) "Show usages of " + editor.document.getText(
                                    TextRange(it.startOffset, it.endOffset)
                                ) else doc, point
                            )
                        } ?: run { logger.warn("Null docrange") }
                    }
                } else {
                    val range = rangeForOffset(offset)
                    createRange(range.startOffset, range.endOffset, getDefinition, visible)
                    invokeLater {
                        docRange?.let {
                            if (it.definitionContainsOffset(offset)) {
                                scheduleShowDoc("Show usages of " + editor.document.getText(TextRange(it.startOffset, it.endOffset)), point)
                            }
                        }
                    }
                }

            }
            prepareDocThread.schedule(prepareDocTask, PREPARE_DOC_THRES.toLong())
        } catch (e: Exception) {
            prepareDocThread = Timer("PrepareDocThread", true)
            logger.warn(e)
        }
    }

    fun canRename(offset: Int = editor.caretModel.currentCaret.offset): Boolean {
        return if (serverOptions.renameOptions.prepareProvider) {
            try {
                val request = requestManager.prepareRename(TextDocumentPositionParams(identifier, offsetToLSPPos(editor, offset)))
                if (request != null) {
                    val result = request.get(PREPARE_RENAME_TIMEOUT(), TimeUnit.MILLISECONDS)
                    if (result != null) {
                        if (result.isLeft) {
                            val range = result.left
                            range != null
                        } else {
                            val renameResult = result.right
                            renameResult != null && renameResult.range != null
                        }
                    } else true
                } else true
            } catch (e: Exception) {
                logger.warn(e)
                true
            }
        } else true
    }

    private fun rangeForOffset(offset: Int): TextRange {
        return try {
            val request = requestManager.documentHighlight(TextDocumentPositionParams(identifier, offsetToLSPPos(editor, offset)))
            if (request != null) {
                val result = request.get(DOC_HIGHLIGHT_TIMEOUT(), TimeUnit.MILLISECONDS)
                if (result != null) {
                    val docH = result.find { dh -> LSPPosToOffset(editor, dh.range.start) <= offset && LSPPosToOffset(editor, dh.range.end) >= offset }
                    if (docH != null) LSPRangeToTextRange(editor, docH.range) else expandOffsetToToken(editor, offset)
                } else expandOffsetToToken(editor, offset)
            } else expandOffsetToToken(editor, offset)
        } catch (e: TimeoutException) {
            wrapper.notifyFailure(Timeouts.DOC_HIGHLIGHT)
            expandOffsetToToken(editor, offset)
        }
    }

    private fun scheduleShowDoc(string: String, point: Point): Unit {
        try {
            val flags = HintManager.HIDE_BY_ANY_KEY or HintManager.HIDE_BY_CARET_MOVE or HintManager.HIDE_BY_ESCAPE or
                    HintManager.HIDE_BY_LOOKUP_ITEM_CHANGE or HintManager.HIDE_BY_OTHER_HINT or HintManager.HIDE_BY_SCROLLING or
                    HintManager.HIDE_BY_TEXT_CHANGE or HintManager.HIDE_IF_OUT_OF_EDITOR
            showDocTask?.cancel()
            showDocThread.purge()
            showDocTask = timerTask {
                invokeLater { currentHint = createAndShowEditorHint(editor, string, point, flags = flags) }
            }
            showDocThread.schedule(showDocTask, SHOW_DOC_THRES)
        } catch (e: Exception) {
            logger.warn(e)
            showDocThread = Timer("ShowDocThread", true)
        }
    }

    /**
     * Immediately requests the server for documentation at the current editor position
     *
     * @param editor The editor
     */
    fun quickDoc(editor: Editor): Unit {
        if (editor == this.editor) {
            val caretPos = editor.caretModel.logicalPosition
            val pointPos = editor.logicalPositionToXY(caretPos)
            pool { requestAndShowDoc(caretPos, pointPos) }
        } else {
            logger.warn("Not same editor!")
        }
    }

    /**
     * Gets the hover request and shows it
     *
     * @param editorPos The editor position
     * @param point     The point at which to show the hint
     */
    private fun requestAndShowDoc(editorPos: LogicalPosition, point: Point): Unit {
        val serverPos = computableReadAction { DocumentUtils.logicalToLSPPos(editorPos, editor) }
        val request = requestManager.hover(TextDocumentPositionParams(identifier, serverPos))
        if (request != null) {
            try {
                val hover = request.get(HOVER_TIMEOUT(), TimeUnit.MILLISECONDS)
                wrapper.notifySuccess(Timeouts.HOVER)
                if (hover != null) {
                    val string = HoverHandler.getHoverString(hover)
                    if (string.isNotEmpty()) {
                        val flags = HintManager.HIDE_BY_ANY_KEY or HintManager.HIDE_BY_CARET_MOVE or HintManager.HIDE_BY_ESCAPE or HintManager.HIDE_BY_LOOKUP_ITEM_CHANGE or
                                HintManager.HIDE_BY_OTHER_HINT or HintManager.HIDE_BY_SCROLLING or HintManager.HIDE_BY_TEXT_CHANGE or HintManager.HIDE_IF_OUT_OF_EDITOR
                        invokeLater { if (!editor.isDisposed) currentHint = createAndShowEditorHint(editor, string, point, flags = flags) }
                    } else {
                        logger.info("Hover string returned is null for file " + identifier.uri + " and pos (" + serverPos.line + ";" + serverPos.character + ")")
                    }
                } else {
                    logger.info("Hover is null for file " + identifier.uri + " and pos (" + serverPos.line + ";" + serverPos.character + ")")
                }
            } catch (e: TimeoutException) {
                logger.warn(e)
                wrapper.notifyFailure(Timeouts.HOVER)
            } catch (e: Exception) {
                e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                    logger.warn(e)
                    wrapper.crashed(e)
                }
            }
        }


    }

    /**
     * Returns the references given the position of the word to search for
     * Must be called from main thread
     *
     * @param offset The offset in the editor
     * @return An array of PsiElement
     */
    fun references(offset: Int, getOriginalElement: Boolean = false, close: Boolean = false): Pair<Iterable<PsiElement>, Iterable<VirtualFile>> {
        val lspPos = offsetToLSPPos(editor, offset)
        val params = ReferenceParams(ReferenceContext(getOriginalElement))
        params.position = lspPos
        params.textDocument = identifier
        val request = requestManager.references(params)
        return if (request != null) {
            try {
                val res = request.get(REFERENCES_TIMEOUT(), TimeUnit.MILLISECONDS)
                wrapper.notifySuccess(Timeouts.REFERENCES)
                if (res != null) {
                    val openedEditors = ArrayList<VirtualFile>()
                    val elements = res.mapNotNull { l ->
                        val start = l.range.start
                        val end = l.range.end
                        val uri = FileUtils.sanitizeURI(l.uri)
                        val file = FileUtils.URIToVFS(uri)
                        var curEditor: Editor? = FileUtils.editorFromUri(uri, project)
                        if (curEditor == null && file != null) {
                            val descriptor = OpenFileDescriptor(project, file)
                            curEditor = computableWriteAction(Computable { FileEditorManager.getInstance(project).openTextEditor(descriptor, false) })
                            openedEditors += file
                        }
                        curEditor?.let {
                            val logicalStart = LSPPosToOffset(it, start)
                            val logicalEnd = LSPPosToOffset(it, end)
                            val name = it.document.getTextClamped(TextRange(logicalStart, logicalEnd))
                            LSPPsiElement(
                                name,
                                project,
                                logicalStart,
                                logicalEnd,
                                PsiDocumentManager.getInstance(project).getPsiFile(it.document)!!,
                                it
                            )
                        } ?: run {
                            logger.warn("Null editor in references")
                            null
                        }
                    }
                    if (close) {
                        writeAction {
                            openedEditors.forEach { f -> FileEditorManager.getInstance(project).closeFile(f) }
                        }
                        openedEditors.clear()
                    }
                    Pair(elements, openedEditors.toList())
                } else {
                    Pair(emptyList(), emptyList())
                }
            } catch (e: TimeoutException) {
                logger.warn(e)
                wrapper.notifyFailure(Timeouts.REFERENCES)
                Pair(emptyList(), emptyList())
            } catch (e: Exception) {
                e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                    logger.warn(e)
                    wrapper.crashed(e)
                    Pair(emptyList(), emptyList())
                }
            }
        } else Pair(emptyList(), emptyList())
    }

    /**
     * Reformat the whole document
     */
    fun reformat(closeAfter: Boolean = false): Unit {
        pool {
            if (!editor.isDisposed) {
                val params = DocumentFormattingParams()
                params.textDocument = (identifier)
                val options = FormattingOptions()
                params.options = (options)
                requestManager.formatting(params)?.thenAccept { formatting ->
                    if (formatting != null) invokeLater {
                        applyEdit(edits = formatting, name = "Reformat document", closeAfter = closeAfter)
                    }
                }
            }
        }
    }

    /**
     * Applies the given edits to the document
     *
     * @param version    The version of the edits (will be discarded if older than current version)
     * @param edits      The edits to apply
     * @param name       The name of the edits (Rename, for example)
     * @param closeAfter will close the file after edits if set to true
     * @return True if the edits were applied, false otherwise
     */
    fun applyEdit(version: Int = Int.MAX_VALUE, edits: Iterable<TextEdit>, name: String = "Apply LSP edits", closeAfter: Boolean = false): Boolean {
        val runnable = getEditsRunnable(version, edits, name)
        writeAction {
            /*      holdDCE.synchronized {
                    holdDCE = true
                  }*/
            if (runnable != null) CommandProcessor.getInstance().executeCommand(project, runnable, name, "LSPPlugin", editor.document)
            if (closeAfter) {
                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
                psiFile?.let {
                    FileEditorManager.getInstance(project)
                        .closeFile(it.virtualFile)
                }
            }
            /*      holdDCE.synchronized {
                    holdDCE = false
                  }*/
        }
        return runnable != null
    }

    /**
     * Returns a Runnable used to apply the given edits and save the document
     * Used by WorkspaceEditHandler (allows to revert a rename for example)
     *
     * @param version The edit version
     * @param edits   The edits
     * @param name    The name of the edit
     * @return The runnable
     */
    fun getEditsRunnable(version: Int = Int.MAX_VALUE, edits: Iterable<TextEdit>, name: String = "Apply LSP edits"): Runnable? {
        return if (version >= this.version) {
            val document = editor.document
            if (document.isWritable) {
                Runnable {
                    edits.map { te ->
                        Pair(
                            document.createRangeMarker(
                                LSPPosToOffset(editor, te.range.start),
                                LSPPosToOffset(editor, te.range.end)
                            ),
                            te.newText
                        )
                    }.forEach { (rangeMarker, text) ->
                        val start = rangeMarker.startOffset
                        val end = rangeMarker.endOffset
                        if (text == "" || text == null) {
                            document.deleteString(start, end)
                        } else if (end - start <= 0) {
                            document.insertString(start, text)
                        } else {
                            document.replaceString(start, end, text)
                        }
                        rangeMarker.dispose()
                    }
                    saveDocument()
                }
            } else {
                logger.warn("Document is not writable")
                null
            }
        } else {
            logger.warn("Edit version " + version + " is older than current version " + this.version)
            null
        }
    }

    private fun saveDocument(): Unit {
        invokeLater { writeAction { FileDocumentManager.getInstance().saveDocument(editor.document) } }
    }

    /**
     * Reformat the text currently selected in the editor
     */
    fun reformatSelection(): Unit {
        pool {
            if (!editor.isDisposed) {
                val params = DocumentRangeFormattingParams()
                params.textDocument = identifier
                val selectionModel = editor.selectionModel
                val start = computableReadAction { selectionModel.selectionStart }
                val end = computableReadAction { selectionModel.selectionEnd }
                val startingPos = offsetToLSPPos(editor, start)
                val endPos = offsetToLSPPos(editor, end)
                params.range = Range(startingPos, endPos)
                val options = FormattingOptions() //TODO
                params.options = options
                val request = requestManager.rangeFormatting(params)
                if (request != null)
                    request.thenAccept { formatting ->
                        if (formatting != null) invokeLater {
                            if (!editor.isDisposed)
                                applyEdit(edits = formatting, name = "Reformat selection")
                        }
                    }
            }
        }
    }

    /**
     * Adds all the listeners
     */
    fun registerListeners(): Unit {
        editor.addEditorMouseListener(mouseListener)
        editor.addEditorMouseMotionListener(mouseMotionListener)
        editor.document.addDocumentListener(documentListener)
        editor.selectionModel.addSelectionListener(selectionListener)
    }

    /**
     * Removes all the listeners
     */
    fun removeListeners(): Unit {
        editor.removeEditorMouseMotionListener(mouseMotionListener)
        editor.document.removeDocumentListener(documentListener)
        editor.removeEditorMouseListener(mouseListener)
        editor.selectionModel.removeSelectionListener(selectionListener)
    }

    /**
     * Rename a symbol in the document
     *
     * @param renameTo The name
     */
    fun rename(renameTo: String, offset: Int = editor.caretModel.currentCaret.offset): Unit {
        pool {
            val servPos = offsetToLSPPos(editor, offset)
            if (!editor.isDisposed) {
                val params = RenameParams(identifier, servPos, renameTo)
                val request = requestManager.rename(params)
                if (request != null) request.thenAccept { res ->
                    WorkspaceEditHandler.applyEdit(res, "Rename to $renameTo", LSPRenameProcessor.getEditors())
                    LSPRenameProcessor.clearEditors()
                }
            }
        }
    }

    fun requestHover(editor: Editor, offset: Int): Hover? {
        return if (editor == this.editor) {
            if (offset != -1) {
                val serverPos = offsetToLSPPos(editor, offset)
                val request = requestManager.hover(TextDocumentPositionParams(identifier, serverPos))
                if (request != null) {
                    try {
                        val response = request.get(HOVER_TIMEOUT(), TimeUnit.MILLISECONDS)
                        wrapper.notifySuccess(Timeouts.HOVER)
                        response
                    } catch (e: TimeoutException) {
                        logger.warn(e)
                        wrapper.notifyFailure(Timeouts.HOVER)
                        null
                    } catch (e: Exception) {
                        e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                            logger.warn(e)
                            wrapper.crashed(e)
                            null
                        }
                    }
                } else {
                    null
                }
            } else {
                logger.warn("Offset at -1")
                null
            }
        } else {
            logger.warn("Not same editor")
            null
        }
    }

    /**
     * Requests the Hover information
     *
     * @param editor The editor
     * @param offset The offset in the editor
     * @return The information
     */
    fun requestDoc(editor: Editor, offset: Int): String {
        val hover = requestHover(editor, offset)
        return if (hover != null) HoverHandler.getHoverString(hover) else ""
    }

    /**
     * Manages the change of selected text in the editor
     *
     * @param e The selection event
     */
    fun selectionChanged(e: SelectionEvent): Unit {
        if (CodeInsightSettings.getInstance().HIGHLIGHT_IDENTIFIER_UNDER_CARET) {
            if (e.editor == editor) {
                selectedSymbHighlights.forEach { h -> editor.markupModel.removeHighlighter(h) }
                selectedSymbHighlights.clear()
                if (editor.selectionModel.hasSelection()) {
                    val ideRange = e.newRange
                    val LSPPos = offsetToLSPPos(editor, (ideRange.endOffset + ideRange.startOffset) / 2)
                    val request = requestManager.documentHighlight(TextDocumentPositionParams(identifier, LSPPos))
                    if (request != null) {
                        pool {
                            if (!editor.isDisposed) {
                                try {
                                    val resp = request.get(DOC_HIGHLIGHT_TIMEOUT(), TimeUnit.MILLISECONDS)
                                    wrapper.notifySuccess(Timeouts.DOC_HIGHLIGHT)
                                    if (resp != null) {
                                        invokeLater {
                                            resp.forEach { dh ->
                                                if (!editor.isDisposed) {
                                                    val range = dh.range
                                                    val kind = dh.kind
                                                    val startOffset = LSPPosToOffset(editor, range.start)
                                                    val endOffset = LSPPosToOffset(editor, range.end)
                                                    val colorScheme = editor.colorsScheme
                                                    val highlight = editor.markupModel.addRangeHighlighter(
                                                        startOffset,
                                                        endOffset,
                                                        HighlighterLayer.SELECTION - 1,
                                                        colorScheme.getAttributes(EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES),
                                                        HighlighterTargetArea.EXACT_RANGE
                                                    )
                                                    selectedSymbHighlights.add(highlight)
                                                }
                                            }
                                        }
                                    }
                                } catch (e: TimeoutException) {
                                    logger.warn(e)
                                    wrapper.notifyFailure(Timeouts.DOC_HIGHLIGHT)
                                } catch (e: Exception) {
                                    e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                                        logger.warn(e)
                                        wrapper.crashed(e)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Tells the manager that the mouse is in the editor
     */
    fun mouseEntered(): Unit {
        mouseInEditor = true
    }

    /**
     * Tells the manager that the mouse is not in the editor
     */
    fun mouseExited(): Unit {
        mouseInEditor = false
        isCtrlDown = false
    }

    fun semanticHighlighting(lines: List<SemanticHighlightingInformation>): Unit {
        val (removeHighlighting, addHighlighting) = lines.partition { s -> s.tokens == null || s.tokens.isNotEmpty() }
        removeHighlighting.forEach { l ->
            val line = l.line
            if (semanticHighlights.contains(line)) {
                semanticHighlights[line]?.forEach { editor.markupModel.removeHighlighter(it) }
            }
            semanticHighlights[line] = emptyList()
        }
        addHighlighting.forEach { l ->
            val line = l.line
            val tokens = SemanticHighlightingTokens.decode(l.tokens)
            val rhs = tokens.map { t ->
                val offset = t.character
                val length = t.length
                val scopes = semanticHighlightingScopes[t.scope] //TODO not sure
                val attributes = scopes.map { scope -> SemanticHighlightingHandler.scopeToTextAttributes(scope, editor) }.head //TODO multiple scopes
                val rh = editor.markupModel.addRangeHighlighter(
                    LSPPosToOffset(editor, Position(line, offset)),
                    LSPPosToOffset(editor, Position(line, offset + length)), HighlighterLayer.SYNTAX,
                    attributes, HighlighterTargetArea.EXACT_RANGE
                )
                rh
            }
            semanticHighlights[line] = rhs
        }
    }

    /**
     * Formats the document when a trigger character is typed
     *
     * @param c The trigger character
     */
    private fun onTypeFormatting(c: String): Unit {
        pool {
            if (!editor.isDisposed) {
                val params = DocumentOnTypeFormattingParams()
                params.ch = c
                params.position = DocumentUtils.logicalToLSPPos(editor.caretModel.currentCaret.logicalPosition, editor)
                params.textDocument = identifier
                params.options = FormattingOptions()
                val future = requestManager.onTypeFormatting(params)
                if (future != null) {
                    try {
                        val edits = future.get(FORMATTING_TIMEOUT(), TimeUnit.MILLISECONDS)
                        wrapper.notifySuccess(Timeouts.FORMATTING)
                        if (edits != null) invokeLater { applyEdit(edits = edits, name = "On type formatting") }
                    } catch (e: TimeoutException) {
                        logger.warn(e)
                        wrapper.notifyFailure(Timeouts.FORMATTING)
                    } catch (e: Exception) {
                        e.multicatch(IOException::class, JsonRpcException::class, ExecutionException::class) {
                            logger.warn(e)
                            wrapper.crashed(e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the logical position given a mouse event
     *
     * @param e The event
     * @return The position (or null if out of bounds)
     */
    private fun getPos(e: EditorMouseEvent): LogicalPosition? {
        val mousePos = e.mouseEvent.point
        val editorPos = editor.xyToLogicalPosition(mousePos)
        val doc = e.editor.document
        val maxLines = doc.lineCount
        return if (editorPos.line >= maxLines) {
            null
        } else {
            val minY = doc.getLineStartOffset(editorPos.line) - (if (editorPos.line > 0) doc.getLineEndOffset(editorPos.line - 1) else 0)
            val maxY = doc.getLineEndOffset(editorPos.line) - (if (editorPos.line > 0) doc.getLineEndOffset(editorPos.line - 1) else 0)
            if (editorPos.column < minY || editorPos.column > maxY) {
                null
            } else {
                editorPos
            }
        }
    }
}