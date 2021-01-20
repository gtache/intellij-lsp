package com.github.gtache.lsp.editor

import java.awt._
import java.awt.event.{KeyEvent, MouseAdapter, MouseEvent}
import java.io.File
import java.net.URI
import java.util
import java.util.concurrent.{ExecutionException, TimeUnit, TimeoutException}
import java.util.{Timer, TimerTask}

import com.github.gtache.lsp.actions.LSPReferencesAction
import com.github.gtache.lsp.client.languageserver.ServerOptions
import com.github.gtache.lsp.client.languageserver.requestmanager.RequestManager
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapperImpl
import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.contributors.rename.LSPRenameProcessor
import com.github.gtache.lsp.requests.{HoverHandler, SemanticHighlightingHandler, Timeouts, WorkspaceEditHandler}
import com.github.gtache.lsp.settings.LSPState
import com.github.gtache.lsp.utils.ConversionUtils._
import com.github.gtache.lsp.utils.DocumentUtils._
import com.github.gtache.lsp.utils.{DocumentUtils, FileUtils, GUIUtils}
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.lookup._
import com.intellij.codeInsight.template.impl.{TemplateImpl, TextExpression}
import com.intellij.codeInsight.template.{Template, TemplateManager}
import com.intellij.lang.LanguageDocumentation
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event._
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.markup._
import com.intellij.openapi.editor.{Editor, LogicalPosition, ScrollType}
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager, OpenFileDescriptor, TextEditor}
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.psi.{PsiDocumentManager, PsiElement}
import com.intellij.ui.Hint
import com.intellij.uiDesigner.core.{GridConstraints, GridLayoutManager, Spacer}
import javax.swing.{JFrame, JLabel, JPanel}
import org.eclipse.lsp4j._
import org.eclipse.lsp4j.jsonrpc.JsonRpcException
import org.eclipse.lsp4j.util.SemanticHighlightingTokens

import scala.collection.mutable.ArrayBuffer
import scala.collection.{JavaConverters, mutable}
import scala.util.Random

object EditorEventManager {
  private val PREPARE_DOC_THRES = 10 //Time between requests when ctrl is pressed (10ms)
  private val SHOW_DOC_THRES: Long = EditorSettingsExternalizable.getInstance().getQuickDocOnMouseOverElementDelayMillis - PREPARE_DOC_THRES

  private val uriToManager: mutable.Map[String, EditorEventManager] = mutable.HashMap()
  private val editorToManager: mutable.Map[Editor, EditorEventManager] = mutable.HashMap()

  @volatile private var isKeyPressed = false
  @volatile private var isCtrlDown = false
  @volatile private var docRange: RangeMarker = _

  KeyboardFocusManager.getCurrentKeyboardFocusManager.addKeyEventDispatcher((e: KeyEvent) => this.synchronized {
    e.getID match {
      case KeyEvent.KEY_PRESSED =>
        isKeyPressed = true
        if (e.getKeyCode == KeyEvent.VK_CONTROL) isCtrlDown = true
      case KeyEvent.KEY_RELEASED =>
        isKeyPressed = false
        if (e.getKeyCode == KeyEvent.VK_CONTROL) {
          isCtrlDown = false
          editorToManager.keys.foreach(e => e.getContentComponent.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)))
          if (docRange != null) docRange.dispose()
          docRange = null
        }
      case _ =>
    }
    false
  })

  /**
    * @param uri A file uri
    * @return The manager for the given uri, or None
    */
  def forUri(uri: String): Option[EditorEventManager] = {
    prune()
    uriToManager.get(uri)
  }

  private def prune(): Unit = {
    editorToManager.filter(e => !e._2.wrapper.isActive).keys.foreach(editorToManager.remove)
    uriToManager.filter(e => !e._2.wrapper.isActive).keys.foreach(uriToManager.remove)
  }

  /**
    * @param editor An editor
    * @return The manager for the given editor, or None
    */
  def forEditor(editor: Editor): Option[EditorEventManager] = {
    prune()
    editorToManager.get(editor)
  }

  /**
    * Tells the server that all the documents will be saved
    */
  def willSaveAll(): Unit = {
    prune()
    editorToManager.foreach(e => e._2.willSave())
  }
}

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
class EditorEventManager(val editor: Editor, val mouseListener: EditorMouseListener, val mouseMotionListener: EditorMouseMotionListener,
                         val documentListener: DocumentListener, val selectionListener: SelectionListener,
                         val requestManager: RequestManager, val serverOptions: ServerOptions, val wrapper: LanguageServerWrapperImpl) {

  import EditorEventManager._
  import GUIUtils.createAndShowEditorHint
  import com.github.gtache.lsp.requests.Timeout._
  import com.github.gtache.lsp.utils.ApplicationUtils._

  import scala.collection.JavaConverters._

  private val identifier: TextDocumentIdentifier = new TextDocumentIdentifier(FileUtils.editorToURIString(editor))
  private val LOG: Logger = Logger.getInstance(classOf[EditorEventManager])
  private val changesParams = new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(), new util.ArrayList[TextDocumentContentChangeEvent]())
  private val selectedSymbHighlights: mutable.Set[RangeHighlighter] = mutable.HashSet()
  private val diagnosticsHighlights: mutable.Set[DiagnosticRangeHighlighter] = mutable.HashSet()
  private val semanticHighlights: mutable.Map[Int, Seq[RangeHighlighter]] = mutable.Map()
  private val syncKind = serverOptions.syncKind

  private val completionTriggers =
    if (serverOptions.completionOptions != null && serverOptions.completionOptions.getTriggerCharacters != null)
      serverOptions.completionOptions.getTriggerCharacters.asScala.toSet.filter(s => s != ".")
    else Set[String]()

  private val signatureTriggers =
    if (serverOptions.signatureHelpOptions != null && serverOptions.signatureHelpOptions.getTriggerCharacters != null)
      serverOptions.signatureHelpOptions.getTriggerCharacters.asScala.toSet
    else Set[String]()

  private val documentOnTypeFormattingOptions = serverOptions.documentOnTypeFormattingOptions
  private val onTypeFormattingTriggers =
    if (documentOnTypeFormattingOptions != null && documentOnTypeFormattingOptions.getMoreTriggerCharacter != null)
      (documentOnTypeFormattingOptions.getMoreTriggerCharacter.asScala += documentOnTypeFormattingOptions.getFirstTriggerCharacter).toSet
    else if (documentOnTypeFormattingOptions != null)
      Set(documentOnTypeFormattingOptions.getFirstTriggerCharacter)
    else
      Set[String]()

  private val semanticHighlightingScopes: IndexedSeq[Seq[String]] =
    if (serverOptions.semanticHighlightingOptions != null && serverOptions.semanticHighlightingOptions.getScopes != null)
      serverOptions.semanticHighlightingOptions.getScopes.asScala.toList.map(l => l.asScala.toList).toIndexedSeq else null

  private val project: Project = editor.getProject
  @volatile var needSave = false
  private var showDocThread = new Timer("ShowDocThread", true)
  private var prepareDocThread = new Timer("PrepareDocThread", true)
  private var showDocTask: TimerTask = _
  private var prepareDocTask: TimerTask = _
  private var version: Int = 0
  private var isOpen: Boolean = false
  private var mouseInEditor: Boolean = true
  private var currentHint: Hint = _
  private var currentDoc: String = _
  private var holdDCE: Boolean = false
  private val DCEs: ArrayBuffer[DocumentEvent] = ArrayBuffer()

  uriToManager.put(FileUtils.editorToURIString(editor), this)
  editorToManager.put(editor, this)
  changesParams.getTextDocument.setUri(identifier.getUri)

  /**
    * Calls onTypeFormatting or signatureHelp if the character typed was a trigger character
    *
    * @param c The character just typed
    */
  def characterTyped(c: Char): Unit = {
    if (completionTriggers.contains(c.toString)) {
      completion(DocumentUtils.offsetToLSPPos(editor, editor.getCaretModel.getCurrentCaret.getOffset))
    } else if (signatureTriggers.contains(c.toString)) {
      signatureHelp()
    } else if (onTypeFormattingTriggers.contains(c.toString)) {
      onTypeFormatting(c.toString)
    }
  }

  /**
    * Calls signatureHelp at the current editor caret position
    */
  def signatureHelp(): Unit = {
    val lPos = editor.getCaretModel.getCurrentCaret.getLogicalPosition
    val point = editor.logicalPositionToXY(lPos)
    val params = new TextDocumentPositionParams(identifier, DocumentUtils.logicalToLSPPos(lPos, editor))
    pool(() => {
      if (!editor.isDisposed) {
        val future = requestManager.signatureHelp(params)
        if (future != null) {
          try {
            val signature = future.get(SIGNATURE_TIMEOUT, TimeUnit.MILLISECONDS)
            wrapper.notifySuccess(Timeouts.SIGNATURE)
            if (signature != null) {
              val signatures = signature.getSignatures
              if (signatures != null && !signatures.isEmpty) {
                val scalaSignatures = signatures.asScala
                val activeSignatureIndex = signature.getActiveSignature
                val activeParameterIndex = signature.getActiveParameter
                val activeParameterLabel = scalaSignatures(activeSignatureIndex).getParameters.get(activeParameterIndex).getLabel
                val activeParameter = if (activeParameterLabel.isLeft) activeParameterLabel.getLeft else
                  scalaSignatures(activeSignatureIndex).getLabel.substring(activeParameterLabel.getRight.getFirst, activeParameterLabel.getRight.getSecond)
                val builder = StringBuilder.newBuilder
                builder.append("<html>")
                scalaSignatures.take(activeSignatureIndex).foreach(sig => builder.append(sig.getLabel).append("<br>"))
                builder.append("<b>").append(scalaSignatures(activeSignatureIndex).getLabel
                  .replace(activeParameter, "<font color=\"yellow\">" + activeParameter + "</font>")).append("</b>")
                scalaSignatures.drop(activeSignatureIndex + 1).foreach(sig => builder.append("<br>").append(sig.getLabel))
                builder.append("</html>")
                val flags = HintManager.HIDE_BY_ESCAPE | HintManager.HIDE_BY_OTHER_HINT | HintManager.HIDE_IF_OUT_OF_EDITOR
                invokeLater(() => currentHint = createAndShowEditorHint(editor, builder.toString(), point, HintManager.UNDER, flags = flags))
              }
            }
          } catch {
            case e: TimeoutException =>
              LOG.warn(e)
              wrapper.notifyFailure(Timeouts.SIGNATURE)
            case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
              LOG.warn(e)
              wrapper.crashed(e.asInstanceOf[Exception])
          }
        }
      }
    })
  }

  /**
    * Retrieves the commands needed to apply a CodeAction
    *
    * @param element The element which needs the CodeAction
    * @return The list of commands, or null if none are given / the request times out
    */
  def codeAction(element: LSPPsiElement): Iterable[jsonrpc.messages.Either[Command, CodeAction]] = {
    val params = new CodeActionParams()
    params.setTextDocument(identifier)
    val range = new Range(DocumentUtils.offsetToLSPPos(editor, element.start), DocumentUtils.offsetToLSPPos(editor, element.end))
    params.setRange(range)
    val context = new CodeActionContext(diagnosticsHighlights.map(_.diagnostic).toList.asJava)
    params.setContext(context)
    val future = requestManager.codeAction(params)
    if (future != null) {
      try {
        val res = future.get(CODEACTION_TIMEOUT, TimeUnit.MILLISECONDS).asScala
        wrapper.notifySuccess(Timeouts.CODEACTION)
        res
      } catch {
        case e: TimeoutException =>
          LOG.warn(e)
          wrapper.notifyFailure(Timeouts.CODEACTION)
          null
        case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
          LOG.warn(e)
          wrapper.crashed(e.asInstanceOf[Exception])
          null
      }

    } else {
      null
    }
  }

  /**
    * Returns the completion suggestions given a position
    *
    * @param pos The LSP position
    * @return The suggestions
    */
  def completion(pos: Position): Iterable[_ <: LookupElement] = {
    val request = requestManager.completion(new CompletionParams(identifier, pos))
    if (request != null) {
      try {
        val res = request.get(COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS)
        wrapper.notifySuccess(Timeouts.COMPLETION)
        if (res != null) {
          import scala.collection.JavaConverters._
          val completion /*: CompletionList | List[CompletionItem] */ = if (res.isLeft) res.getLeft.asScala else res.getRight

          /**
            * Creates a LookupElement given a CompletionItem
            *
            * @param item The CompletionItem
            * @return The corresponding LookupElement
            */
          def createLookupItem(item: CompletionItem): LookupElement = {
            def execCommand(command: Command): Unit = {
              if (command != null) executeCommands(Iterable(command))
            }

            def prepareTemplate(insertText: String): Template = {
              val startIndexes = 0.until(insertText.length).filter(insertText.startsWith("$", _))
              val variables = startIndexes.map(i => {
                val sub = insertText.drop(i + 1)
                if (sub.head == '{') {
                  val num = sub.tail.takeWhile(c => c != ':')
                  val placeholder = sub.tail.dropWhile(c => c != ':').tail.takeWhile(c => c != '}')
                  val len = num.length + placeholder.length + 4
                  (i, i + len, num, placeholder)
                } else {
                  val num = sub.takeWhile(c => c.isDigit)
                  val placeholder = "..."
                  val len = num.length + 1
                  (i, i + len, num, placeholder)
                }
              })
              var newInsertText = insertText
              variables.sortBy(t => -t._1).foreach(t => newInsertText = newInsertText.take(t._1) + "$" + t._3 + "$" + newInsertText.drop(t._2))

              val template = TemplateManager.getInstance(project).createTemplate("anon" + (1 to 5).map(_ => Random.nextPrintableChar()).mkString(""), "lsp")

              variables.foreach(t => {
                template.addVariable(t._3, new TextExpression(t._4), new TextExpression(t._4), true, false)
              })
              template.setInline(true)
              template.asInstanceOf[TemplateImpl].setString(newInsertText)
              template
            }


            //TODO improve
            val addTextEdits = item.getAdditionalTextEdits
            val command = item.getCommand
            val commitChars = item.getCommitCharacters
            val data = item.getData
            val deprecated = item.getDeprecated
            val detail = item.getDetail
            val doc = item.getDocumentation
            val filterText = item.getFilterText
            val insertText = item.getInsertText
            val insertFormat = item.getInsertTextFormat //TODO snippet
            val kind = item.getKind
            val label = item.getLabel
            val textEdit = item.getTextEdit
            val sortText = item.getSortText
            val presentableText = if (label != null && label != "") label else if (insertText != null) insertText else ""
            val tailText = if (detail != null) "\t" + detail else ""
            val iconProvider = GUIUtils.getIconProviderFor(wrapper.getServerDefinition)
            val icon = iconProvider.getCompletionIcon(kind)
            var lookupElementBuilder: LookupElementBuilder = null
            /*            .withRenderer((element: LookupElement, presentation: LookupElementPresentation) => { //TODO later
                      presentation match {
                        case realPresentation: RealLookupElementPresentation =>
                          if (!realPresentation.hasEnoughSpaceFor(presentation.getItemText, presentation.isItemTextBold)) {
                          }
                      }
                    })*/

            def runSnippet(template: Template): Unit = {
              invokeLater(() => {
                writeAction(() => CommandProcessor.getInstance().executeCommand(project, () => editor.getDocument.insertString(editor.getCaretModel.getOffset, template.getTemplateText), "snippetInsert", "lsp", editor.getDocument))
                TemplateManager.getInstance(project).startTemplate(editor, template)
                if (addTextEdits != null) {
                  applyEdit(edits = addTextEdits.asScala, name = "Additional Completions : " + label)
                }
                execCommand(command)
              })
            }

            def applyEdits(edit: Seq[TextEdit], moveToCaret: Boolean, textToBeReplaced : String = ""): Unit = {
              invokeLater(() => {
                if (edit != null && edit.nonEmpty) {
                  applyEdit(edits = edit, name = "Completion : " + label)
                }
                execCommand(command)
                if (moveToCaret) {
                  editor.getCaretModel.moveCaretRelatively(textEdit.getNewText.length - textToBeReplaced.length, 0, false, false, true)
                }
              })
            }

            if (textEdit != null) {
              if (addTextEdits != null) {
                lookupElementBuilder = LookupElementBuilder.create(presentableText, "")
                  .withInsertHandler((context: InsertionContext, _: LookupElement) => {
                    context.commitDocument()
                    if (insertFormat == InsertTextFormat.Snippet) {
                      val template = prepareTemplate(textEdit.getNewText)
                      runSnippet(template)
                    } else {
                      applyEdits(textEdit +: addTextEdits.asScala, moveToCaret = true)
                    }
                  })
                  .withLookupString(presentableText)
              } else {
                // Get current range as offset
                val offsetStart = editor.getDocument.getLineStartOffset(pos.getLine) + textEdit.getRange.getStart.getCharacter;
                val offsetEnd = editor.getDocument.getLineStartOffset(pos.getLine) + textEdit.getRange.getEnd.getCharacter;

                // Get text to be replaced
                val textToBeReplaced = editor.getDocument.getText(new TextRange(offsetStart, offsetEnd))

                lookupElementBuilder = LookupElementBuilder.create(presentableText, textToBeReplaced)
                  .withInsertHandler((context: InsertionContext, _: LookupElement) => {
                    context.commitDocument()
                    if (insertFormat == InsertTextFormat.Snippet) {
                      val template = prepareTemplate(textEdit.getNewText)
                      runSnippet(template)
                    } else {
                      applyEdits(Seq(textEdit), moveToCaret = true, textToBeReplaced)
                    }
                  })
                  .withLookupString(presentableText)
              }
            } else if (addTextEdits != null) {
              lookupElementBuilder = LookupElementBuilder.create(presentableText, "")
                .withInsertHandler((context: InsertionContext, _: LookupElement) => {
                  context.commitDocument()
                  if (insertFormat == InsertTextFormat.Snippet) {
                    val template = prepareTemplate(if (insertText != null && insertText != "") insertText else label)
                    runSnippet(template)
                  } else {
                    applyEdits(addTextEdits.asScala, moveToCaret = false)
                  }
                })
                .withLookupString(presentableText)
            } else {
              lookupElementBuilder = LookupElementBuilder.create(if (insertText != null && insertText != "") insertText else label)
              if (command != null) lookupElementBuilder = lookupElementBuilder.withInsertHandler((context: InsertionContext, _: LookupElement) => {
                context.commitDocument()
                if (insertFormat == InsertTextFormat.Snippet) {
                  val template = prepareTemplate(if (insertText != null && insertText != "") insertText else label)
                  runSnippet(template)
                }
                applyEdits(Seq.empty, moveToCaret = false)
              })
            }
            if (kind == CompletionItemKind.Keyword) lookupElementBuilder = lookupElementBuilder.withBoldness(true)
            if (deprecated) {
              lookupElementBuilder = lookupElementBuilder.withStrikeoutness(true)
            }
            lookupElementBuilder.withPresentableText(presentableText).withTailText(tailText, true).withIcon(icon).withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT)
          }

          completion match {
            case c: CompletionList =>
              c.getItems.asScala.map(item => {
                createLookupItem(item)
              })
            case l: Iterable[CompletionItem@unchecked] => l.map(item => {
              createLookupItem(item)
            })
          }
        } else Iterable()
      }
      catch {
        case e: TimeoutException =>
          LOG.warn(e)
          wrapper.notifyFailure(Timeouts.COMPLETION)
          Iterable.empty
        case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
          LOG.warn(e)
          wrapper.crashed(e.asInstanceOf[Exception])
          Iterable.empty
      }
    } else Iterable.empty
  }

  /**
    * Sends commands to execute to the server and applies the changes returned if the future returns a WorkspaceEdit
    *
    * @param commands The commands to execute
    */
  def executeCommands(commands: Iterable[Command]): Unit = {
    pool(() => {
      if (!editor.isDisposed) {
        commands.map(c => {
          requestManager.executeCommand(new ExecuteCommandParams(c.getCommand, c.getArguments))
        }).foreach(f => {
          if (f != null) {
            try {
              val ret = f.get(EXECUTE_COMMAND_TIMEOUT, TimeUnit.MILLISECONDS)
              wrapper.notifySuccess(Timeouts.EXECUTE_COMMAND)
              ret match {
                case e: WorkspaceEdit => WorkspaceEditHandler.applyEdit(e, name = "Execute command")
                case _ =>
                  LOG.warn("ExecuteCommand returned " + ret)
              }
            } catch {
              case e: TimeoutException =>
                LOG.warn(e)
                wrapper.notifyFailure(Timeouts.EXECUTE_COMMAND)
              case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
                LOG.warn(e)
                wrapper.crashed(e.asInstanceOf[Exception])
            }
          }
        })
      }
    })
  }

  /**
    * Applies the diagnostics to the document
    *
    * @param diagnostics The diagnostics to apply from the server
    */
  def diagnostics(diagnostics: Iterable[Diagnostic]): Unit = {
    def rangeToOffsets(range: Range): TextRange = {
      val (start, end) = (DocumentUtils.LSPPosToOffset(editor, range.getStart), DocumentUtils.LSPPosToOffset(editor, range.getEnd))
      if (start == end) {
        DocumentUtils.expandOffsetToToken(editor, start)
      } else {
        new TextRange(start, end)
      }
    }

    invokeLater(() => {
      if (!editor.isDisposed) {
        diagnosticsHighlights.synchronized {
          diagnosticsHighlights.foreach(highlight => editor.getMarkupModel.removeHighlighter(highlight.rangeHighlighter))
          diagnosticsHighlights.clear()
        }
        for (diagnostic <- diagnostics) {
          val range = diagnostic.getRange
          val severity = diagnostic.getSeverity

          val markupModel = editor.getMarkupModel
          val colorScheme = editor.getColorsScheme

          val (effectType, effectColor, layer) = severity match {
            case null => null
            case DiagnosticSeverity.Error => (EffectType.WAVE_UNDERSCORE, java.awt.Color.RED, HighlighterLayer.ERROR)
            case DiagnosticSeverity.Warning => (EffectType.WAVE_UNDERSCORE, java.awt.Color.YELLOW, HighlighterLayer.WARNING)
            case DiagnosticSeverity.Information => (EffectType.WAVE_UNDERSCORE, java.awt.Color.GRAY, HighlighterLayer.WARNING)
            case DiagnosticSeverity.Hint => (EffectType.BOLD_DOTTED_LINE, java.awt.Color.GRAY, HighlighterLayer.WARNING)
          }

          val textRange = rangeToOffsets(range)
          val start = textRange.getStartOffset
          val end = textRange.getEndOffset
          diagnosticsHighlights.synchronized {
            diagnosticsHighlights
              .add(DiagnosticRangeHighlighter(markupModel.addRangeHighlighter(start, end, layer,
                new TextAttributes(colorScheme.getDefaultForeground, colorScheme.getDefaultBackground, effectColor, effectType, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE),
                diagnostic))
          }
        }
      }
    })
  }

  private def releaseDCE(): Unit = {
    DCEs.synchronized {
      if (!editor.isDisposed) {
        changesParams.synchronized {
          val changeEvent = new TextDocumentContentChangeEvent()
          syncKind match {
            case TextDocumentSyncKind.None =>
            case TextDocumentSyncKind.Full =>
              changeEvent.setText(editor.getDocument.getText)
              changesParams.getContentChanges.add(changeEvent)
            case TextDocumentSyncKind.Incremental =>
              DCEs.filter(e => e.getDocument == editor.getDocument).map(event => {
                val newText = event.getNewFragment
                val offset = event.getOffset
                val newTextLength = event.getNewLength
                val lspPosition: Position = DocumentUtils.offsetToLSPPos(editor, offset)
                val startLine = lspPosition.getLine
                val startColumn = lspPosition.getCharacter
                val oldText = event.getOldFragment

                //if text was deleted/replaced, calculate the end position of inserted/deleted text
                val (endLine, endColumn) = if (oldText.length() > 0) {
                  val line = startLine + StringUtil.countNewLines(oldText)
                  val oldLines = oldText.toString.split('\n')
                  val oldTextLength = if (oldLines.isEmpty) 0 else oldLines.last.length
                  val column = if (oldLines.length == 1) startColumn + oldTextLength else oldTextLength
                  (line, column)
                } else (startLine, startColumn) //if insert or no text change, the end position is the same
                val range = new Range(new Position(startLine, startColumn), new Position(endLine, endColumn))
                changeEvent.setRange(range)
                changeEvent.setRangeLength(newTextLength)
                changeEvent.setText(newText.toString)
                changeEvent
              }).foreach(changesParams.getContentChanges.add(_))
          }
        }
        requestManager.didChange(changesParams)
        changesParams.getContentChanges.clear()
      }
    }
  }

  private def cancelDoc(): Unit = {
    try {
      if (currentHint != null) currentHint.hide()
      currentHint = null
      if (prepareDocTask != null) prepareDocTask.cancel()
      if (showDocTask != null) showDocTask.cancel()
      if (docRange != null) docRange.dispose()
      docRange = null
    } catch {
      case e: Exception => LOG.warn(e)
    }
  }

  /**
    * Handles the DocumentChanged events
    *
    * @param event The DocumentEvent
    */
  def documentChanged(event: DocumentEvent): Unit = {
    if (holdDCE) {
      DCEs.synchronized {
        DCEs.append(event)
      }
    } else {
      if (!editor.isDisposed) {
        if (event.getDocument == editor.getDocument) {
          cancelDoc()
          changesParams.synchronized {
            changesParams.getTextDocument.setVersion({
              version += 1
              version
            })
            val changeEvent = new TextDocumentContentChangeEvent()
            syncKind match {
              case TextDocumentSyncKind.None =>
              case TextDocumentSyncKind.Incremental =>
                val newText = event.getNewFragment
                val offset = event.getOffset
                val newTextLength = event.getNewLength
                val lspPosition: Position = DocumentUtils.offsetToLSPPos(editor, offset)
                val startLine = lspPosition.getLine
                val startColumn = lspPosition.getCharacter
                val oldText = event.getOldFragment.toString
                //if text was deleted/replaced, calculate the end position of inserted/deleted text
                val (endLine, endColumn) = if (oldText.length() > 0) {
                  val line = startLine + StringUtil.countNewLines(oldText)
                  val oldLines = oldText.split('\n')
                  val oldTextLength = if (oldLines.isEmpty) 0 else oldLines.last.length
                  val column = if (oldText.endsWith("\n")) 0 else if (oldLines.length == 1) startColumn + oldTextLength else oldTextLength
                  (line, column)
                } else (startLine, startColumn) //if insert or no text change, the end position is the same
                val range = new Range(new Position(startLine, startColumn), new Position(endLine, endColumn))
                changeEvent.setRange(range)
                changeEvent.setRangeLength(DocumentUtils.LSPPosToOffset(editor, range.getEnd) - DocumentUtils.LSPPosToOffset(editor, range.getStart))
                changeEvent.setText(newText.toString)
                changesParams.getContentChanges.add(changeEvent)

              case TextDocumentSyncKind.Full =>
                changeEvent.setText(editor.getDocument.getText)
                changesParams.getContentChanges.add(changeEvent)
            }
            requestManager.didChange(changesParams)
            changesParams.getContentChanges.clear()
          }
        } else {
          LOG.error("Wrong document for the EditorEventManager")
        }
      }
    }
  }

  /**
    * Notifies the server that the corresponding document has been closed
    */
  def documentClosed(): Unit = {
    pool(() => {
      if (isOpen) {
        requestManager.didClose(new DidCloseTextDocumentParams(identifier))
        isOpen = false
        editorToManager.remove(editor)
        uriToManager.remove(FileUtils.editorToURIString(editor))
      } else {
        LOG.warn("Editor " + identifier.getUri + " was already closed")
      }
    })
  }

  def documentOpened(): Unit = {
    pool(() => {
      if (!editor.isDisposed) {
        if (isOpen) {
          LOG.warn("Editor " + editor + " was already open")
        } else {
          requestManager.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(identifier.getUri, wrapper.serverDefinition.id, version, editor.getDocument.getText)))
          isOpen = true
        }
      }
    })
  }

  /**
    * Notifies the server that the corresponding document has been saved
    */
  def documentSaved(): Unit = {
    pool(() => {
      if (!editor.isDisposed) {
        val params: DidSaveTextDocumentParams = new DidSaveTextDocumentParams(identifier, editor.getDocument.getText)
        requestManager.didSave(params)
      }
    })
  }

  /**
    * Indicates that the document will be saved
    */
  //TODO Manual
  def willSave(): Unit = {
    if (wrapper.isWillSaveWaitUntil && !needSave) willSaveWaitUntil() else pool(() => {
      if (!editor.isDisposed) requestManager.willSave(new WillSaveTextDocumentParams(identifier, TextDocumentSaveReason.Manual))
    })
  }

  /**
    * If the server supports willSaveWaitUntil, the LSPVetoer will check if  a save is needed
    * (needSave will basically alterate between true or false, so the document will always be saved)
    */
  private def willSaveWaitUntil(): Unit = {
    if (wrapper.isWillSaveWaitUntil) {
      pool(() => {
        if (!editor.isDisposed) {
          val params = new WillSaveTextDocumentParams(identifier, TextDocumentSaveReason.Manual)
          val future = requestManager.willSaveWaitUntil(params)
          if (future != null) {
            try {
              val edits = future.get(WILLSAVE_TIMEOUT, TimeUnit.MILLISECONDS)
              wrapper.notifySuccess(Timeouts.WILLSAVE)
              if (edits != null) {
                invokeLater(() => applyEdit(edits = edits.asScala, name = "WaitUntil edits"))
              }
            } catch {
              case e: TimeoutException =>
                LOG.warn(e)
                wrapper.notifyFailure(Timeouts.WILLSAVE)
              case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
                LOG.warn(e)
                wrapper.crashed(e.asInstanceOf[Exception])
            } finally {
              needSave = true
              saveDocument()
            }
          } else {
            needSave = true
            saveDocument()
          }
        }
      })
    } else {
      LOG.error("Server doesn't support WillSaveWaitUntil")
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
  def documentReferences(offset: Int): Iterable[(Int, Int)] = {
    val params = new ReferenceParams()
    val context = new ReferenceContext()
    context.setIncludeDeclaration(true)
    params.setContext(context)
    params.setPosition(DocumentUtils.offsetToLSPPos(editor, offset))
    params.setTextDocument(identifier)
    val future = requestManager.references(params)
    if (future != null) {
      try {
        val references = future.get(REFERENCES_TIMEOUT, TimeUnit.MILLISECONDS)
        wrapper.notifySuccess(Timeouts.REFERENCES)
        if (references != null) {
          references.asScala.collect {
            case l: Location if FileUtils.sanitizeURI(l.getUri) == identifier.getUri =>
              (DocumentUtils.LSPPosToOffset(editor, l.getRange.getStart), DocumentUtils.LSPPosToOffset(editor, l.getRange.getEnd))
          }
        } else {
          null
        }
      } catch {
        case e: TimeoutException =>
          LOG.warn(e)
          wrapper.notifyFailure(Timeouts.REFERENCES)
          null
        case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
          LOG.warn(e)
          wrapper.crashed(e.asInstanceOf[Exception])
          null
      }
    }
    else {
      null
    }
  }

  /**
    * @return The current diagnostics highlights
    */
  def getDiagnostics: mutable.Set[DiagnosticRangeHighlighter] = {
    diagnosticsHighlights.clone()
  }

  def getElementAtOffset(offset: Int): LSPPsiElement = {
    computableReadAction(() => {
      if (!editor.isDisposed) {
        val params = new TextDocumentPositionParams(identifier, DocumentUtils.offsetToLSPPos(editor, offset))
        val future = requestManager.documentHighlight(params)
        if (future != null) {
          try {
            val res = future.get(DOC_HIGHLIGHT_TIMEOUT, TimeUnit.MILLISECONDS)
            wrapper.notifySuccess(Timeouts.DOC_HIGHLIGHT)
            if (res != null && !editor.isDisposed)
              res.asScala.map(dh => new TextRange(DocumentUtils.LSPPosToOffset(editor, dh.getRange.getStart), DocumentUtils.LSPPosToOffset(editor, dh.getRange.getEnd)))
                .find(range => range.getStartOffset <= offset && offset <= range.getEndOffset)
                .map(range => LSPPsiElement(editor.getDocument.getText(range), project, range.getStartOffset, range.getEndOffset, PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument), editor))
                .orNull
            else null
          } catch {
            case e: TimeoutException =>
              wrapper.notifyFailure(Timeouts.DOC_HIGHLIGHT)
              LOG.warn(e)
              null
            case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
              LOG.warn(e)
              wrapper.crashed(e.asInstanceOf[Exception])
              null
          }
        } else null
      }
      else null
    })
  }

  /**
    * Called when the mouse is clicked
    * At the moment, is used by CTRL+click to see references / goto definition
    *
    * @param e The mouse event
    */
  def mouseClicked(e: EditorMouseEvent): Unit = {
    if (e.getMouseEvent.isControlDown && docRange != null && docRange.loc != null && docRange.loc.getUri != null) {
      val loc = docRange.loc
      val offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(e.getMouseEvent.getPoint))
      val locUri = FileUtils.sanitizeURI(loc.getUri)
      if (identifier.getUri == locUri) {
        if (docRange.definitionContainsOffset(offset)) {
          ActionManager.getInstance().getAction("LSPFindUsages").asInstanceOf[LSPReferencesAction].forManagerAndOffset(this, offset)
        } else {
          val startOffset = DocumentUtils.LSPPosToOffset(editor, loc.getRange.getStart)
          writeAction(() => {
            editor.getCaretModel.moveToOffset(startOffset)
            editor.getSelectionModel.setSelection(startOffset, DocumentUtils.LSPPosToOffset(editor, loc.getRange.getEnd))
            editor.getScrollingModel.scrollToCaret(ScrollType.CENTER);
          })
        }
      } else {
        val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(locUri)))
        if (file != null) {
          val descriptor = new OpenFileDescriptor(project, file)
          writeAction(() => {
            val newEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            val startOffset = DocumentUtils.LSPPosToOffset(newEditor, loc.getRange.getStart)
            newEditor.getCaretModel.getCurrentCaret.moveToOffset(startOffset)
            newEditor.getSelectionModel.setSelection(startOffset, DocumentUtils.LSPPosToOffset(newEditor, loc.getRange.getEnd))
            newEditor.getScrollingModel.scrollToCaret(ScrollType.CENTER);
          })
        } else {
          LOG.warn("Empty file for " + locUri)
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
  def showReferences(offset: Int): Unit = {
    invokeLater(() => {
      if (!editor.isDisposed) {
        writeAction(() => editor.getCaretModel.getCurrentCaret.moveToOffset(offset))
        showReferences(includeDefinition = false)
      }
    })
  }

  /**
    * Queries references and show a window with these references (click on a row to get to the location)
    */
  def showReferences(includeDefinition: Boolean = true): Unit = {
    pool(() => {
      if (!editor.isDisposed) {
        val context = new ReferenceContext(includeDefinition)
        val params = new ReferenceParams(context)
        params.setTextDocument(identifier)
        val serverPos = computableReadAction(() => {
          DocumentUtils.logicalToLSPPos(editor.getCaretModel.getCurrentCaret.getLogicalPosition, editor)
        })
        params.setPosition(serverPos)
        val future = requestManager.references(params)
        if (future != null) {
          try {
            val references = future.get(REFERENCES_TIMEOUT, TimeUnit.MILLISECONDS)
            wrapper.notifySuccess(Timeouts.REFERENCES)
            if (references != null) {
              invokeLater(() => {
                if (!editor.isDisposed) showReferences(references.asScala)
              })
            }
          } catch {
            case e: TimeoutException =>
              LOG.warn(e)
              wrapper.notifyFailure(Timeouts.REFERENCES)
            case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
              LOG.warn(e)
              wrapper.crashed(e.asInstanceOf[Exception])
          }
        }
      }
    })
  }

  private def showReferences(references: Iterable[Location]): Unit = {
    var name: String = ""

    /**
      * Opens the editor and get the required infos
      *
      * @param file              The file of the editor
      * @param fileEditorManager The fileEditorManager
      * @param start             The starting position
      * @param end               The ending position
      * @return (StartOffset, EndOffset, Name (of the symbol), line (containing the symbol))
      */
    def openEditorAndGetOffsetsAndName(file: VirtualFile, fileEditorManager: FileEditorManager, start: Position, end: Position): (Int, Int, String, String) = {
      val descriptor = new OpenFileDescriptor(project, file)
      computableWriteAction(() => {
        val newEditor = fileEditorManager.openTextEditor(descriptor, false)
        val startOffset = DocumentUtils.LSPPosToOffset(newEditor, start)
        val endOffset = DocumentUtils.LSPPosToOffset(newEditor, end)
        val doc = newEditor.getDocument
        val name = doc.getTextClamped(new TextRange(startOffset, endOffset))
        fileEditorManager.closeFile(file)
        (startOffset, endOffset, name, DocumentUtils.getLineText(newEditor, startOffset, endOffset))
      })
    }

    val locations = references.map(l => {
      val start = l.getRange.getStart
      val end = l.getRange.getEnd
      val uri = FileUtils.sanitizeURI(l.getUri)
      var startOffset: Int = -1
      var endOffset: Int = -1
      var sample: String = ""

      /**
        * Opens the editor to retrieve the offsets, line, etc if needed
        */
      def manageUnopenedEditor(): Unit = {
        val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(uri)))
        val fileEditorManager = FileEditorManager.getInstance(project)
        if (fileEditorManager.isFileOpen(file)) {
          val editors = fileEditorManager.getAllEditors(file).collect { case t: TextEditor => t.getEditor }
          if (editors.isEmpty) {
            val (s, e, n, sa) = openEditorAndGetOffsetsAndName(file, fileEditorManager, start, end)
            startOffset = s
            endOffset = e
            name = n
            sample = sa
          } else {
            startOffset = DocumentUtils.LSPPosToOffset(editors.head, start)
            endOffset = DocumentUtils.LSPPosToOffset(editors.head, end)
          }
        } else {
          val (s, e, n, sa) = openEditorAndGetOffsetsAndName(file, fileEditorManager, start, end)
          startOffset = s
          endOffset = e
          name = n
          sample = sa
        }
      }

      EditorEventManager.forUri(uri) match {
        case Some(m) =>
          try {
            startOffset = DocumentUtils.LSPPosToOffset(m.editor, start)
            endOffset = DocumentUtils.LSPPosToOffset(m.editor, end)
            name = m.editor.getDocument.getTextClamped(new TextRange(startOffset, endOffset))
            sample = DocumentUtils.getLineText(m.editor, startOffset, endOffset)
          } catch {
            case e: RuntimeException =>
              LOG.warn(e)
              manageUnopenedEditor()
          }
        case None =>
          manageUnopenedEditor()
      }

      (uri, startOffset, endOffset, sample)
    }).toArray

    val caretPoint = editor.logicalPositionToXY(editor.getCaretModel.getCurrentCaret.getLogicalPosition)
    showReferencesWindow(locations, name, caretPoint)
  }

  /**
    * Creates and shows the references window given the locations
    *
    * @param locations The locations : The file URI, the start offset, end offset, and the sample (line) containing the offsets
    * @param name      The name of the symbol
    * @param point     The point at which to show the window
    */
  private def showReferencesWindow(locations: Array[(String, Int, Int, String)], name: String, point: Point): Unit = {
    if (locations.isEmpty) {
      val flags = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_CARET_MOVE | HintManager.HIDE_BY_ESCAPE | HintManager.HIDE_BY_LOOKUP_ITEM_CHANGE |
        HintManager.HIDE_BY_OTHER_HINT | HintManager.HIDE_BY_SCROLLING | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_IF_OUT_OF_EDITOR
      invokeLater(() => if (!editor.isDisposed) currentHint = createAndShowEditorHint(editor, "No usages found", point, flags = flags))
    } else {
      val frame = new JFrame()
      frame.setTitle("Usages of " + name + " (" + locations.length + (if (locations.length > 1) " usages found)" else " usage found"))
      val panel = new JPanel()
      var row = 0
      panel.setLayout(new GridLayoutManager(locations.length, 4, new Insets(10, 10, 10, 10), -1, -1))
      locations.foreach(l => {
        val listener = new MouseAdapter() {
          override def mouseClicked(e: MouseEvent): Unit = {
            val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(FileUtils.sanitizeURI(l._1))))
            val descriptor = new OpenFileDescriptor(project, file, l._2)
            writeAction(() => {
              val newEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
              if (l._2 != -1 && l._3 != -1) newEditor.getSelectionModel.setSelection(l._2, l._3)
            })
            frame.setVisible(false)
            frame.dispose()
          }
        }
        val fileLabel = new JLabel(new File(new URI(FileUtils.sanitizeURI(l._1))).getName)
        val spacer = new Spacer()
        val offsetLabel = new JLabel(l._2.toString)
        val sampleLabel = new JLabel("<html>" + l._4 + "</html>")
        panel.add(fileLabel, new GridConstraints(row, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
        panel.add(spacer, new GridConstraints(row, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false))
        panel.add(offsetLabel, new GridConstraints(row, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
        panel.add(sampleLabel, new GridConstraints(row, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
        row += 1
        //TODO refine
        fileLabel.addMouseListener(listener)
        spacer.addMouseListener(listener)
        offsetLabel.addMouseListener(listener)
        sampleLabel.addMouseListener(listener)
      })
      panel.setVisible(true)
      frame.setContentPane(panel)

      frame.setLocationRelativeTo(editor.getContentComponent)
      frame.setLocation(point)
      frame.pack()
      frame.setAutoRequestFocus(true)
      frame.setAlwaysOnTop(true)
      frame.setVisible(true)
    }
  }

  private def createRange(startOffset: Int, endOffset: Int, getDefinition: Boolean = false, visible: Boolean = true): Unit = {
    val loc = if (getDefinition) requestDefinition(offsetToLSPPos(editor, (endOffset + startOffset) / 2)) else null
    val isDefinition = loc != null && DocumentUtils.LSPPosToOffset(editor, loc.getTargetRange.getStart) == startOffset
    invokeLater(() => {
      if (docRange != null) docRange.dispose()
      if (!editor.isDisposed) {
        val range = if (!isDefinition && visible) editor.getMarkupModel.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.HYPERLINK, editor.getColorsScheme.getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR), HighlighterTargetArea.EXACT_RANGE) else null
        docRange = RangeMarker(startOffset, endOffset, editor, loc, range, isDefinition)
      }
    })
  }

  /**
    * Returns the position of the definition given a position in the editor
    *
    * @param position The position
    * @return The location of the definition
    */
  private def requestDefinition(position: Position): LocationLink = {
    val params = new TextDocumentPositionParams(identifier, position)
    val request = requestManager.definition(params)
    if (request != null) {
      try {
        val definition = request.get(DEFINITION_TIMEOUT, TimeUnit.MILLISECONDS)
        wrapper.notifySuccess(Timeouts.DEFINITION)
        if (definition != null) {
          if (definition.isLeft) {
            val left = definition.getLeft
            if (left != null && !left.isEmpty) {
              val loc = left.get(0)
              if (loc != null) loc else null
            } else null
          } else {
            val right = definition.getRight
            if (right != null && !right.isEmpty) {
              val locLink = right.get(0)
              if (locLink != null) locLink else null
            } else null
          }
        } else null
      } catch {
        case e: TimeoutException =>
          LOG.warn(e)
          wrapper.notifyFailure(Timeouts.DEFINITION)
          null
        case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
          LOG.warn(e)
          wrapper.crashed(e.asInstanceOf[Exception])
          null
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
  def mouseMoved(e: EditorMouseEvent): Unit = {
    if (e.getEditor == editor) {
      val ctrlDown = e.getMouseEvent.isControlDown
      val language = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument).getLanguage
      if ((LSPState.getInstance().isAlwaysSendRequests || LanguageDocumentation.INSTANCE.allForLanguage(language).isEmpty || language.equals(PlainTextLanguage.INSTANCE))
        && (ctrlDown || EditorSettingsExternalizable.getInstance().isShowQuickDocOnMouseOverElement)) {
        val lPos = getPos(e)
        if (lPos != null) {
          val offset = editor.logicalPositionToOffset(lPos)
          if (docRange == null || !docRange.highlightContainsOffset(offset)) {
            cancelDoc()
            scheduleCreateRange(offsetToLSPPos(editor, offset), e.getMouseEvent.getPoint, getDefinition = ctrlDown, visible = ctrlDown)
          } else {
            if (!ctrlDown) {
              scheduleShowDoc(currentDoc, e.getMouseEvent.getPoint)
              editor.getContentComponent.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR))
            } else {
              editor.getContentComponent.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
              if (docRange.loc == null) {
                cancelDoc()
                scheduleCreateRange(offsetToLSPPos(editor, offset), e.getMouseEvent.getPoint, getDefinition = true, visible = true)
              }
            }
          }
        }
      }
    } else {
      LOG.error("Wrong editor for EditorEventManager")
    }
  }

  private def scheduleCreateRange(serverPos: Position, point: Point, getDefinition: Boolean = true, visible: Boolean = true): Unit = {
    try {
      if (prepareDocTask != null) prepareDocTask.cancel()
      prepareDocThread.purge()
      prepareDocTask = new TimerTask {
        override def run(): Unit = {
          val offset = LSPPosToOffset(editor, serverPos)
          val hover = requestHover(editor, offset)
          if (hover != null) {
            val doc = HoverHandler.getHoverString(hover)
            currentDoc = doc
            val range = if (hover.getRange == null) getRangeForOffset(offset) else LSPRangeToTextRange(editor, hover.getRange)
            if (range != null) {
              createRange(range.getStartOffset, range.getEndOffset, getDefinition = getDefinition, visible = visible)
              invokeLater(() =>
                scheduleShowDoc(if (docRange.definitionContainsOffset(offset)) "Show usages of " + editor.getDocument.getText(new TextRange(docRange.startOffset, docRange.endOffset)) else doc, point))
            }
          } else {
            val range = getRangeForOffset(offset)
            if (range != null) {
              createRange(range.getStartOffset, range.getEndOffset, getDefinition, visible)
              invokeLater(() =>
                if (docRange.definitionContainsOffset(offset)) {
                  scheduleShowDoc("Show usages of " + editor.getDocument.getText(new TextRange(docRange.startOffset, docRange.endOffset)), point)
                })
            }
          }
        }
      }
      prepareDocThread.schedule(prepareDocTask, PREPARE_DOC_THRES)
    } catch {
      case e: Exception =>
        prepareDocThread = new Timer("PrepareDocThread", true)
        LOG.warn(e)
    }
  }

  def canRename(offset: Int = editor.getCaretModel.getCurrentCaret.getOffset): Boolean = {
    if (serverOptions.renameOptions.getPrepareProvider) {
      try {
        val request = requestManager.prepareRename(new TextDocumentPositionParams(identifier, offsetToLSPPos(editor, offset)))
        if (request != null) {
          val result = request.get(PREPARE_RENAME_TIMEOUT, TimeUnit.MILLISECONDS)
          if (result != null) {
            if (result.isLeft) {
              val range = result.getLeft
              LOG.warn(range.toString)
              range != null
            } else {
              val renameResult = result.getRight
              LOG.warn(renameResult.toString)
              renameResult != null && renameResult.getRange != null
            }
          } else true
        } else true
      } catch {
        case e: Exception =>
          LOG.warn(e)
          true
      }
    } else true
  }

  private def getRangeForOffset(offset: Int): TextRange = {
    try {
      val request = requestManager.documentHighlight(new TextDocumentPositionParams(identifier, offsetToLSPPos(editor, offset)))
      if (request != null) {
        val result = request.get(DOC_HIGHLIGHT_TIMEOUT, TimeUnit.MILLISECONDS)
        if (result != null) {
          result.asScala.find(dh => LSPPosToOffset(editor, dh.getRange.getStart) <= offset && LSPPosToOffset(editor, dh.getRange.getEnd) >= offset).map(dh => LSPRangeToTextRange(editor, dh.getRange)).orNull
        } else expandOffsetToToken(editor, offset)
      } else expandOffsetToToken(editor, offset)
    } catch {
      case e: TimeoutException =>
        wrapper.notifyFailure(Timeouts.DOC_HIGHLIGHT)
        expandOffsetToToken(editor, offset)
    }
  }

  private def scheduleShowDoc(string: String, point: Point): Unit = {
    try {
      val flags = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_CARET_MOVE | HintManager.HIDE_BY_ESCAPE |
        HintManager.HIDE_BY_LOOKUP_ITEM_CHANGE | HintManager.HIDE_BY_OTHER_HINT | HintManager.HIDE_BY_SCROLLING |
        HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_IF_OUT_OF_EDITOR
      if (showDocTask != null) showDocTask.cancel()
      showDocThread.purge()
      showDocTask = new TimerTask {
        override def run(): Unit = {
          invokeLater(() => currentHint = createAndShowEditorHint(editor, string, point, flags = flags))
        }
      }
      showDocThread.schedule(showDocTask, SHOW_DOC_THRES)
    } catch {
      case e: Exception =>
        LOG.warn(e)
        showDocThread = new Timer("ShowDocThread", true)
    }
  }

  /**
    * Immediately requests the server for documentation at the current editor position
    *
    * @param editor The editor
    */
  def quickDoc(editor: Editor): Unit = {
    if (editor == this.editor) {
      val caretPos = editor.getCaretModel.getLogicalPosition
      val pointPos = editor.logicalPositionToXY(caretPos)
      val currentTime = System.nanoTime()
      pool(() => requestAndShowDoc(caretPos, pointPos))
    } else {
      LOG.warn("Not same editor!")
    }
  }

  /**
    * Gets the hover request and shows it
    *
    * @param editorPos The editor position
    * @param point     The point at which to show the hint
    */
  private def requestAndShowDoc(editorPos: LogicalPosition, point: Point): Unit = {
    val serverPos = computableReadAction[Position](() => DocumentUtils.logicalToLSPPos(editorPos, editor))
    val request = requestManager.hover(new TextDocumentPositionParams(identifier, serverPos))
    if (request != null) {
      try {
        val hover = request.get(HOVER_TIMEOUT, TimeUnit.MILLISECONDS)
        wrapper.notifySuccess(Timeouts.HOVER)
        if (hover != null) {
          val string = HoverHandler.getHoverString(hover)
          if (string != null && string != "") {
            val flags = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_CARET_MOVE | HintManager.HIDE_BY_ESCAPE | HintManager.HIDE_BY_LOOKUP_ITEM_CHANGE |
              HintManager.HIDE_BY_OTHER_HINT | HintManager.HIDE_BY_SCROLLING | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_IF_OUT_OF_EDITOR
            invokeLater(() => if (!editor.isDisposed) currentHint = createAndShowEditorHint(editor, string, point, flags = flags))
          } else {
            LOG.info("Hover string returned is null for file " + identifier.getUri + " and pos (" + serverPos.getLine + ";" + serverPos.getCharacter + ")")
          }
        } else {
          LOG.info("Hover is null for file " + identifier.getUri + " and pos (" + serverPos.getLine + ";" + serverPos.getCharacter + ")")
        }
      } catch {
        case e: TimeoutException =>
          LOG.warn(e)
          wrapper.notifyFailure(Timeouts.HOVER)
        case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
          LOG.warn(e)
          wrapper.crashed(e.asInstanceOf[Exception])
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
  def references(offset: Int, getOriginalElement: Boolean = false, close: Boolean = false): (Iterable[PsiElement], Iterable[VirtualFile]) = {
    val lspPos = DocumentUtils.offsetToLSPPos(editor, offset)
    val params = new ReferenceParams(new ReferenceContext(getOriginalElement))
    params.setPosition(lspPos)
    params.setTextDocument(identifier)
    val request = requestManager.references(params)
    if (request != null) {
      try {
        val res = request.get(REFERENCES_TIMEOUT, TimeUnit.MILLISECONDS)
        wrapper.notifySuccess(Timeouts.REFERENCES)
        if (res != null) {
          val openedEditors = mutable.ListBuffer[VirtualFile]()
          val elements = res.asScala.map(l => {
            val start = l.getRange.getStart
            val end = l.getRange.getEnd
            val uri = FileUtils.sanitizeURI(l.getUri)
            val file = FileUtils.URIToVFS(uri)
            var curEditor = FileUtils.editorFromUri(uri, project)
            if (curEditor == null) {
              val descriptor = new OpenFileDescriptor(project, file)
              curEditor = computableWriteAction(() => FileEditorManager.getInstance(project).openTextEditor(descriptor, false))
              openedEditors += file
            }
            val logicalStart = DocumentUtils.LSPPosToOffset(curEditor, start)
            val logicalEnd = DocumentUtils.LSPPosToOffset(curEditor, end)
            val name = curEditor.getDocument.getTextClamped(new TextRange(logicalStart, logicalEnd))
            LSPPsiElement(name, project, logicalStart, logicalEnd, FileUtils.psiFileFromEditor(curEditor), curEditor)
              .asInstanceOf[PsiElement]
          })
          if (close) {
            writeAction(() => openedEditors.foreach(f => FileEditorManager.getInstance(project).closeFile(f)))
            openedEditors.clear()
          }
          (elements, openedEditors.clone())
        } else {
          (Seq.empty, Seq.empty)
        }
      } catch {
        case e: TimeoutException =>
          LOG.warn(e)
          wrapper.notifyFailure(Timeouts.REFERENCES)
          (Seq.empty, Seq.empty)
        case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
          LOG.warn(e)
          wrapper.crashed(e.asInstanceOf[Exception])
          (Seq.empty, Seq.empty)
      }
    } else (Seq.empty, Seq.empty)
  }

  /**
    * Reformat the whole document
    */
  def reformat(closeAfter: Boolean = false): Unit = {
    pool(() => {
      if (!editor.isDisposed) {
        val params = new DocumentFormattingParams()
        params.setTextDocument(identifier)
        val options = new FormattingOptions()
        params.setOptions(options)
        val request = requestManager.formatting(params)
        if (request != null) request.thenAccept(formatting => if (formatting != null) invokeLater(() =>
          applyEdit(edits = formatting.asScala, name = "Reformat document", closeAfter = closeAfter)))
      }
    })
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
  def applyEdit(version: Int = Int.MaxValue, edits: Iterable[TextEdit], name: String = "Apply LSP edits", closeAfter: Boolean = false): Boolean = {
    val runnable = getEditsRunnable(version, edits, name)
    writeAction(() => {
      /*      holdDCE.synchronized {
              holdDCE = true
            }*/
      if (runnable != null) CommandProcessor.getInstance().executeCommand(project, runnable, name, "LSPPlugin", editor.getDocument)
      if (closeAfter) {
        FileEditorManager.getInstance(project)
          .closeFile(PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument).getVirtualFile)
      }
      /*      holdDCE.synchronized {
              holdDCE = false
            }*/
    })
    if (runnable != null) true else false
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
  def getEditsRunnable(version: Int = Int.MaxValue, edits: Iterable[TextEdit], name: String = "Apply LSP edits"): Runnable = {
    if (version >= this.version) {
      val document = editor.getDocument
      if (document.isWritable) {
        () => {
          edits.map(te => {
            (document.createRangeMarker(DocumentUtils.LSPPosToOffset(editor, te.getRange.getStart),
              DocumentUtils.LSPPosToOffset(editor, te.getRange.getEnd)),
              te.getNewText)
          }).foreach(markerText => {
            val start = markerText._1.getStartOffset
            val end = markerText._1.getEndOffset
            val text = markerText._2
            if (text == "" || text == null) {
              document.deleteString(start, end)
            } else if (end - start <= 0) {
              document.insertString(start, text)
            } else {
              document.replaceString(start, end, text)
            }
            markerText._1.dispose()
          })
          saveDocument()
        }
      } else {
        LOG.warn("Document is not writable")
        null
      }
    } else {
      LOG.warn("Edit version " + version + " is older than current version " + this.version)
      null
    }
  }

  private def saveDocument(): Unit = {
    invokeLater(() => writeAction(() => FileDocumentManager.getInstance().saveDocument(editor.getDocument)))
  }

  /**
    * Reformat the text currently selected in the editor
    */
  def reformatSelection(): Unit = {
    pool(() => {
      if (!editor.isDisposed) {
        val params = new DocumentRangeFormattingParams()
        params.setTextDocument(identifier)
        val selectionModel = editor.getSelectionModel
        val start = computableReadAction(() => selectionModel.getSelectionStart)
        val end = computableReadAction(() => selectionModel.getSelectionEnd)
        val startingPos = DocumentUtils.offsetToLSPPos(editor, start)
        val endPos = DocumentUtils.offsetToLSPPos(editor, end)
        params.setRange(new Range(startingPos, endPos))
        val options = new FormattingOptions() //TODO
        params.setOptions(options)
        val request = requestManager.rangeFormatting(params)
        if (request != null)
          request.thenAccept(formatting =>
            if (formatting != null) invokeLater(() =>
              if (!editor.isDisposed)
                applyEdit(edits = formatting.asScala, name = "Reformat selection")))
      }
    })
  }

  /**
    * Adds all the listeners
    */
  def registerListeners(): Unit = {
    editor.addEditorMouseListener(mouseListener)
    editor.addEditorMouseMotionListener(mouseMotionListener)
    editor.getDocument.addDocumentListener(documentListener)
    editor.getSelectionModel.addSelectionListener(selectionListener)
  }

  /**
    * Removes all the listeners
    */
  def removeListeners(): Unit = {
    editor.removeEditorMouseMotionListener(mouseMotionListener)
    editor.getDocument.removeDocumentListener(documentListener)
    editor.removeEditorMouseListener(mouseListener)
    editor.getSelectionModel.removeSelectionListener(selectionListener)
  }

  /**
    * Rename a symbol in the document
    *
    * @param renameTo The new name
    */
  def rename(renameTo: String, offset: Int = editor.getCaretModel.getCurrentCaret.getOffset): Unit = {
    pool(() => {
      val servPos = DocumentUtils.offsetToLSPPos(editor, offset)
      if (!editor.isDisposed) {
        val params = new RenameParams(identifier, servPos, renameTo)
        val request = requestManager.rename(params)
        if (request != null) request.thenAccept(res => {
          WorkspaceEditHandler.applyEdit(res, "Rename to " + renameTo, LSPRenameProcessor.getEditors.toList)
          LSPRenameProcessor.clearEditors()
        })
      }
    })
  }

  def requestHover(editor: Editor, offset: Int): Hover = {
    if (editor == this.editor) {
      if (offset != -1) {
        val serverPos = DocumentUtils.offsetToLSPPos(editor, offset)
        val request = requestManager.hover(new TextDocumentPositionParams(identifier, serverPos))
        if (request != null) {
          try {
            val response = request.get(HOVER_TIMEOUT, TimeUnit.MILLISECONDS)
            wrapper.notifySuccess(Timeouts.HOVER)
            response
          } catch {
            case e: TimeoutException =>
              LOG.warn(e)
              wrapper.notifyFailure(Timeouts.HOVER)
              null
            case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
              LOG.warn(e)
              wrapper.crashed(e.asInstanceOf[Exception])
              null
          }
        } else {
          null
        }
      } else {
        LOG.warn("Offset at -1")
        null
      }
    } else {
      LOG.warn("Not same editor")
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
  def requestDoc(editor: Editor, offset: Int): String = {
    val hover = requestHover(editor, offset)
    if (hover != null) HoverHandler.getHoverString(hover) else ""
  }

  /**
    * Manages the change of selected text in the editor
    *
    * @param e The selection event
    */
  def selectionChanged(e: SelectionEvent): Unit = {
    if (CodeInsightSettings.getInstance().HIGHLIGHT_IDENTIFIER_UNDER_CARET) {
      if (e.getEditor == editor) {
        selectedSymbHighlights.foreach(h => editor.getMarkupModel.removeHighlighter(h))
        selectedSymbHighlights.clear()
        if (editor.getSelectionModel.hasSelection) {
          val ideRange = e.getNewRange
          val LSPPos = DocumentUtils.offsetToLSPPos(editor, (ideRange.getEndOffset + ideRange.getStartOffset) / 2)
          val request = requestManager.documentHighlight(new TextDocumentPositionParams(identifier, LSPPos))
          if (request != null) {
            pool(() => {
              if (!editor.isDisposed) {
                try {
                  val resp = request.get(DOC_HIGHLIGHT_TIMEOUT, TimeUnit.MILLISECONDS)
                  wrapper.notifySuccess(Timeouts.DOC_HIGHLIGHT)
                  if (resp != null) {
                    invokeLater(() => resp.asScala.foreach(dh => {
                      if (!editor.isDisposed) {
                        val range = dh.getRange
                        val kind = dh.getKind
                        val startOffset = DocumentUtils.LSPPosToOffset(editor, range.getStart)
                        val endOffset = DocumentUtils.LSPPosToOffset(editor, range.getEnd)
                        val colorScheme = editor.getColorsScheme
                        val highlight = editor.getMarkupModel.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION - 1, colorScheme.getAttributes(EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES), HighlighterTargetArea.EXACT_RANGE)
                        selectedSymbHighlights.add(highlight)
                      }
                    }))
                  }
                } catch {
                  case e: TimeoutException =>
                    LOG.warn(e)
                    wrapper.notifyFailure(Timeouts.DOC_HIGHLIGHT)
                  case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
                    LOG.warn(e)
                    wrapper.crashed(e.asInstanceOf[Exception])
                }
              }
            })
          }
        }
      }
    }
  }

  /**
    * Tells the manager that the mouse is in the editor
    */
  def mouseEntered(): Unit = {
    mouseInEditor = true
  }

  /**
    * Tells the manager that the mouse is not in the editor
    */
  def mouseExited(): Unit = {
    mouseInEditor = false
    isCtrlDown = false
  }

  def semanticHighlighting(lines: Seq[SemanticHighlightingInformation]): Unit = {
    val (removeHighlighting, addHighlighting) = lines.partition(s => s.getTokens == null || s.getTokens.nonEmpty)
    removeHighlighting.foreach(l => {
      val line = l.getLine
      if (semanticHighlights.contains(line)) {
        semanticHighlights(line).foreach(editor.getMarkupModel.removeHighlighter)
      }
      semanticHighlights(line) = Seq.empty
    })
    addHighlighting.foreach(l => {
      val line = l.getLine
      val tokens = JavaConverters.asScalaBuffer(SemanticHighlightingTokens.decode(l.getTokens))
      val rhs = tokens.map(t => {
        val offset = t.character
        val length = t.length
        val scopes = semanticHighlightingScopes(t.scope) //TODO not sure
        val attributes = scopes.map(scope => SemanticHighlightingHandler.scopeToTextAttributes(scope, editor)).head //TODO multiple scopes
        val rh = editor.getMarkupModel.addRangeHighlighter(DocumentUtils.LSPPosToOffset(editor, new Position(line, offset)),
          DocumentUtils.LSPPosToOffset(editor, new Position(line, offset + length)), HighlighterLayer.SYNTAX,
          attributes, HighlighterTargetArea.EXACT_RANGE)
        rh
      })
      semanticHighlights(line) = rhs
    })
  }

  /**
    * Formats the document when a trigger character is typed
    *
    * @param c The trigger character
    */
  private def onTypeFormatting(c: String): Unit = {
    pool(() => {
      if (!editor.isDisposed) {
        val params = new DocumentOnTypeFormattingParams()
        params.setCh(c)
        params.setPosition(DocumentUtils.logicalToLSPPos(editor.getCaretModel.getCurrentCaret.getLogicalPosition, editor))
        params.setTextDocument(identifier)
        params.setOptions(new FormattingOptions())
        val future = requestManager.onTypeFormatting(params)
        if (future != null) {
          try {
            val edits = future.get(FORMATTING_TIMEOUT, TimeUnit.MILLISECONDS)
            wrapper.notifySuccess(Timeouts.FORMATTING)
            if (edits != null) invokeLater(() => applyEdit(edits = edits.asScala, name = "On type formatting"))
          } catch {
            case e: TimeoutException =>
              LOG.warn(e)
              wrapper.notifyFailure(Timeouts.FORMATTING)
            case e@(_: java.io.IOException | _: JsonRpcException | _: ExecutionException) =>
              LOG.warn(e)
              wrapper.crashed(e.asInstanceOf[Exception])
          }
        }
      }
    })
  }

  /**
    * Returns the logical position given a mouse event
    *
    * @param e The event
    * @return The position (or null if out of bounds)
    */
  private def getPos(e: EditorMouseEvent): LogicalPosition = {
    val mousePos = e.getMouseEvent.getPoint
    val editorPos = editor.xyToLogicalPosition(mousePos)
    val doc = e.getEditor.getDocument
    val maxLines = doc.getLineCount
    if (editorPos.line >= maxLines) {
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

  def getFoldingRanges: Array[FoldingDescriptor] = {
    try {
      val request = requestManager.foldingRange(new FoldingRangeRequestParams(identifier))
      if (request != null) {
        val result = request.get(FOLDING_RANGE_TIMEOUT, TimeUnit.MILLISECONDS)
        if (result != null) {
          result.asScala.map(fr => {
            val startOffset = if (fr.getStartCharacter != null) LSPPosToOffset(editor, new Position(fr.getStartLine, fr.getStartCharacter)) else editor.getDocument.getLineEndOffset(fr.getStartLine)
            val endOffset = if (fr.getEndCharacter != null) LSPPosToOffset(editor, new Position(fr.getEndLine, fr.getEndCharacter)) else editor.getDocument.getLineEndOffset(fr.getEndLine)
            new FoldingDescriptor(LSPPsiElement("foldingRange", project, startOffset, endOffset, FileUtils.psiFileFromEditor(editor), editor), new TextRange(startOffset, endOffset))
          }).toArray
        } else Array()
      } else Array()
    } catch {
      case e: Exception =>
        LOG.warn(e)
        Array()
    }
  }
}
