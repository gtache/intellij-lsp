package com.github.gtache.lsp.editor

import java.awt._
import java.awt.event.{KeyEvent, MouseAdapter, MouseEvent}
import java.io.File
import java.net.URI
import java.util.concurrent.{TimeUnit, TimeoutException}
import java.util.{Collections, Timer, TimerTask}
import javax.swing.{JFrame, JLabel, JPanel}

import com.github.gtache.lsp.client.languageserver.ServerOptions
import com.github.gtache.lsp.client.languageserver.requestmanager.RequestManager
import com.github.gtache.lsp.client.languageserver.wrapper.LanguageServerWrapperImpl
import com.github.gtache.lsp.contributors.psi.LSPPsiElement
import com.github.gtache.lsp.requests.{HoverHandler, WorkspaceEditHandler}
import com.github.gtache.lsp.utils.{DocumentUtils, FileUtils, GUIUtils}
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.lookup._
import com.intellij.lang.LanguageDocumentation
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event._
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.markup._
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.fileEditor.{FileDocumentManager, FileEditorManager, OpenFileDescriptor, TextEditor}
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import com.intellij.psi.{PsiDocumentManager, PsiReference}
import com.intellij.ui.Hint
import com.intellij.uiDesigner.core.{GridConstraints, GridLayoutManager, Spacer}
import org.eclipse.lsp4j._

import scala.collection.mutable

object EditorEventManager {
  private val HOVER_TIME_THRES: Long = EditorSettingsExternalizable.getInstance().getQuickDocOnMouseOverElementDelayMillis * 1000000
  private val SCHEDULE_THRES = 10000000 //Time before the Timer is scheduled
  private val POPUP_THRES = HOVER_TIME_THRES / 1000000 + 20

  private val uriToManager: mutable.Map[String, EditorEventManager] = mutable.HashMap()
  private val editorToManager: mutable.Map[Editor, EditorEventManager] = mutable.HashMap()

  @volatile private var isKeyPressed = false
  @volatile private var isCtrlDown = false
  @volatile private var ctrlRange: CtrlRangeMarker = _

  KeyboardFocusManager.getCurrentKeyboardFocusManager.addKeyEventDispatcher((e: KeyEvent) => this.synchronized {
    if (e.isControlDown) {
      isCtrlDown = true
    } else {
      isCtrlDown = false
      if (ctrlRange != null) ctrlRange.dispose()
      ctrlRange = null
    }
    e.getID match {
      case KeyEvent.KEY_PRESSED => isKeyPressed = true
      case KeyEvent.KEY_RELEASED => isKeyPressed = false
      case _ =>
    }
    false
  })

  /**
    * @param uri A file uri
    * @return The manager for the given uri, or None
    */
  def forUri(uri: String): Option[EditorEventManager] = {
    uriToManager.get(uri)
  }

  /**
    * @param editor An editor
    * @return The manager for the given editor, or None
    */
  def forEditor(editor: Editor): Option[EditorEventManager] = {
    editorToManager.get(editor)
  }

  /**
    * Tells the server that all the documents will be saved
    */
  def willSaveAll(): Unit = {
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
  import GUIUtils.createAndShowHint
  import com.github.gtache.lsp.requests.Timeout._
  import com.github.gtache.lsp.utils.ApplicationUtils._

  import scala.collection.JavaConverters._

  private val identifier: TextDocumentIdentifier = new TextDocumentIdentifier(FileUtils.editorToURIString(editor))
  private val LOG: Logger = Logger.getInstance(classOf[EditorEventManager])
  private val changesParams = new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(), Collections.singletonList(new TextDocumentContentChangeEvent()))
  private val selectedSymbHighlights: mutable.Set[RangeHighlighter] = mutable.HashSet()
  private val diagnosticsHighlights: mutable.Set[DiagnosticRangeHighlighter] = mutable.HashSet()
  private val syncKind = serverOptions.syncKind
  private val completionTriggers = if (serverOptions.completionOptions != null) serverOptions.completionOptions.getTriggerCharacters.asScala.toSet.filter(s => s != ".") else Set[String]()
  private val signatureTriggers = if (serverOptions.signatureHelpOptions != null) serverOptions.signatureHelpOptions.getTriggerCharacters.asScala.toSet else Set[String]()
  private val onTypeFormattingTriggers = if (serverOptions.documentOnTypeFormattingOptions != null)
    (serverOptions.documentOnTypeFormattingOptions.getMoreTriggerCharacter.asScala +: serverOptions.documentOnTypeFormattingOptions.getFirstTriggerCharacter).toSet else Set[String]()
  @volatile var needSave = false
  private var hoverThread = new Timer("Hover", true)
  private var version: Int = -1
  private var predTime: Long = -1L
  private var isOpen: Boolean = true
  private var mouseInEditor: Boolean = true
  private var currentHint: Hint = _

  uriToManager.put(FileUtils.editorToURIString(editor), this)
  editorToManager.put(editor, this)
  changesParams.getTextDocument.setUri(identifier.getUri)
  pool(() => requestManager.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(identifier.getUri, wrapper.serverDefinition.id, {
    version += 1
    version - 1
  }, editor.getDocument.getText))))


  /**
    * Sends commands to execute to the server and applies the changes returned if the future returns a WorkspaceEdit
    *
    * @param commands The commands to execute
    */
  def executeCommands(commands: Iterable[Command]): Unit = {
    pool(() => {
      if (!editor.isDisposed) {
        commands.map(c => {
          val params = new ExecuteCommandParams()
          params.setArguments(c.getArguments)
          params.setCommand(c.getCommand)
          requestManager.executeCommand(params)
        }).foreach(f => {
          if (f != null) {
            try {
              val ret = f.get(EXECUTE_COMMAND_TIMEOUT, TimeUnit.MILLISECONDS)
              ret match {
                case e: WorkspaceEdit => WorkspaceEditHandler.applyEdit(e, name = "Execute command")
                case _ =>
              }
            } catch {
              case e: TimeoutException => LOG.warn(e)
            }
          }
        })
      }
    })
  }

  /**
    * @return The current diagnostics highlights
    */
  def getDiagnostics: mutable.Set[DiagnosticRangeHighlighter] = {
    diagnosticsHighlights.clone()
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
    * Tells the manager that the mouse is in the editor
    */
  def startListening(): Unit = {
    mouseInEditor = true
  }

  /**
    * Tells the manager that the mouse is not in the editor
    */
  def stopListening(): Unit = {
    mouseInEditor = false
  }

  /**
    * Called when the mouse is clicked
    * At the moment, is used by CTRL+click to see references / goto definition
    *
    * @param e The mouse event
    */
  def mouseClicked(e: EditorMouseEvent): Unit = {
    if (ctrlRange != null && isCtrlDown) {
      val loc = ctrlRange.loc
      invokeLater(() => {
        if (!editor.isDisposed) {
          val offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(e.getMouseEvent.getPoint))
          if (identifier.getUri == loc.getUri && DocumentUtils.LSPPosToOffset(editor, loc.getRange.getStart) <= offset && offset <= DocumentUtils.LSPPosToOffset(editor, loc.getRange.getEnd)) {
            getReferences(offset)
          } else {
            val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(loc.getUri).getPath))
            val descriptor = new OpenFileDescriptor(editor.getProject, file)
            writeAction(() => {
              val newEditor = FileEditorManager.getInstance(editor.getProject).openTextEditor(descriptor, true)
              val startOffset = DocumentUtils.LSPPosToOffset(newEditor, loc.getRange.getStart)
              newEditor.getCaretModel.getCurrentCaret.moveToOffset(startOffset)
              newEditor.getSelectionModel.setSelection(startOffset, DocumentUtils.LSPPosToOffset(newEditor, loc.getRange.getEnd))
            })
          }
          ctrlRange.dispose()
          ctrlRange = null
        }
      })
    }
  }

  /**
    * Queries references and show them in a window, given the offset of the symbol in the editor
    *
    * @param offset The offset
    */
  def getReferences(offset: Int): Unit = {
    invokeLater(() => {
      if (!editor.isDisposed) {
        writeAction(() => editor.getCaretModel.getCurrentCaret.moveToOffset(offset))
        getReferences(includeDefinition = false)
      }
    })
  }

  /**
    * Queries references and show a window with these references (click on a row to get to the location)
    */
  def getReferences(includeDefinition: Boolean = true): Unit = {
    pool(() => {
      if (!editor.isDisposed) {
        val context = new ReferenceContext(includeDefinition)
        val params = new ReferenceParams(context)
        params.setTextDocument(identifier)
        val serverPos = computableReadAction(() => {
          DocumentUtils.logicalToLSPPos(editor.getCaretModel.getCurrentCaret.getLogicalPosition)
        })
        params.setPosition(serverPos)
        val future = requestManager.references(params)
        if (future != null) {
          val references = future.get(REFERENCES_TIMEOUT, TimeUnit.MILLISECONDS)
          if (references != null) {
            invokeLater(() => {
              if (!editor.isDisposed) showReferences(references.asScala)
            })
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
      val descriptor = new OpenFileDescriptor(editor.getProject, file)
      computableWriteAction(() => {
        val newEditor = fileEditorManager.openTextEditor(descriptor, false)
        val startOffset = DocumentUtils.LSPPosToOffset(newEditor, start)
        val endOffset = DocumentUtils.LSPPosToOffset(newEditor, end)
        val doc = newEditor.getDocument
        val name = doc.getText(new TextRange(startOffset, endOffset))
        fileEditorManager.closeFile(file)
        (startOffset, endOffset, name, DocumentUtils.getLineText(newEditor, startOffset, endOffset))
      })
    }

    val locations = references.map(l => {
      val start = l.getRange.getStart
      val end = l.getRange.getEnd
      var startOffset: Int = -1
      var endOffset: Int = -1
      var sample: String = ""

      /**
        * Opens the editor to retrieve the offsets, line, etc if needed
        */
      def manageUnopenedEditor(): Unit = {
        val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(l.getUri).getPath))
        val fileEditorManager = FileEditorManager.getInstance(editor.getProject)
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

      EditorEventManager.forUri(l.getUri) match {
        case Some(m) =>
          try {
            startOffset = DocumentUtils.LSPPosToOffset(m.editor, start)
            endOffset = DocumentUtils.LSPPosToOffset(m.editor, end)
            name = m.editor.getDocument.getText(new TextRange(startOffset, endOffset))
            sample = DocumentUtils.getLineText(m.editor, startOffset, endOffset)
          } catch {
            case e: RuntimeException =>
              LOG.warn(e)
              manageUnopenedEditor()
          }
        case None =>
          manageUnopenedEditor()
      }

      (l.getUri, startOffset, endOffset, sample)
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
      invokeLater(() => if (!editor.isDisposed) currentHint = createAndShowHint(editor, "No usages found", point))
    } else {
      val frame = new JFrame()
      frame.setTitle("Usages of " + name + " (" + locations.length + (if (locations.length > 1) " usages found)" else " usage found"))
      val panel = new JPanel()
      var row = 0
      panel.setLayout(new GridLayoutManager(locations.length, 4, new Insets(10, 10, 10, 10), -1, -1))
      locations.foreach(l => {
        val listener = new MouseAdapter() {
          override def mouseClicked(e: MouseEvent): Unit = {
            val file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(l._1).getPath))
            val descriptor = new OpenFileDescriptor(editor.getProject, file, l._2)
            writeAction(() => {
              val newEditor = FileEditorManager.getInstance(editor.getProject).openTextEditor(descriptor, true)
              if (l._2 != -1 && l._3 != -1) newEditor.getSelectionModel.setSelection(l._2, l._3)
            })
            frame.setVisible(false)
            frame.dispose()
          }
        }
        val fileLabel = new JLabel(new File(new URI(l._1).getPath).getName)
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
          val LSPPos = DocumentUtils.offsetToLSPPos(editor, ideRange.getStartOffset)
          val request = requestManager.documentHighlight(new TextDocumentPositionParams(identifier, LSPPos))
          if (request != null) {
            pool(() => {
              if (!editor.isDisposed) {
                try {
                  val resp = request.get(DOC_HIGHLIGHT_TIMEOUT, TimeUnit.MILLISECONDS).asScala
                  invokeLater(() => resp.foreach(dh => {
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
                } catch {
                  case e: TimeoutException =>
                    LOG.warn(e)
                }
              }
            })
          }
        }
      }
    }
  }

  /**
    * Will show documentation if the mouse doesn't move for a given time (Hover)
    *
    * @param e the event
    */
  def mouseMoved(e: EditorMouseEvent): Unit = {
    if (e.getEditor == editor) {
      val language = PsiDocumentManager.getInstance(editor.getProject).getPsiFile(editor.getDocument).getLanguage
      if ((EditorSettingsExternalizable.getInstance().isShowQuickDocOnMouseOverElement && //Fixes double doc if documentation provider is present
        (LanguageDocumentation.INSTANCE.allForLanguage(language).isEmpty || language.equals(PlainTextLanguage.INSTANCE))) || isCtrlDown) {
        val curTime = System.nanoTime()
        if (predTime == (-1L)) {
          predTime = curTime
        } else {
          val lPos = getPos(e)
          if (lPos != null) {
            if (!isKeyPressed || isCtrlDown) {
              val offset = editor.logicalPositionToOffset(lPos)
              if (isCtrlDown) {
                if (ctrlRange == null || !ctrlRange.containsOffset(offset)) {
                  if (currentHint != null) currentHint.hide()
                  currentHint = null
                  if (ctrlRange != null) ctrlRange.dispose()
                  ctrlRange = null
                  pool(() => requestAndShowDoc(curTime, lPos, e.getMouseEvent.getPoint))
                }
              } else {
                scheduleDocumentation(curTime, lPos, e.getMouseEvent.getPoint)
              }

            }
          }
          predTime = curTime
        }
      }
    } else {
      LOG.error("Wrong editor for EditorEventManager")
    }
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

  /**
    * Schedule the documentation using the Timer
    *
    * @param time      The current time
    * @param editorPos The position in the editor
    * @param point     The point where to show the doc
    */
  private def scheduleDocumentation(time: Long, editorPos: LogicalPosition, point: Point): Unit = {
    if (editorPos != null) {
      if (time - predTime > SCHEDULE_THRES) {
        try {
          hoverThread.schedule(new TimerTask {
            override def run(): Unit = {
              if (!editor.isDisposed) {
                val curTime = System.nanoTime()
                if (curTime - predTime > HOVER_TIME_THRES && mouseInEditor && editor.getContentComponent.hasFocus && (!isKeyPressed || isCtrlDown)) {
                  val editorOffset = computableReadAction[Int](() => editor.logicalPositionToOffset(editorPos))
                  val inHighlights = diagnosticsHighlights.filter(diag =>
                    diag.rangeHighlighter.getStartOffset <= editorOffset &&
                      editorOffset <= diag.rangeHighlighter.getEndOffset)
                  if (inHighlights.isEmpty || isCtrlDown) {
                    requestAndShowDoc(curTime, editorPos, point)
                  }
                }
              }
            }
          }, POPUP_THRES)
        } catch {
          case e: Exception =>
            hoverThread = new Timer("Hover", true) //Restart Timer if it crashes
            LOG.warn(e)
            LOG.warn("Hover timer reset")
        }
      }
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
      pool(() => requestAndShowDoc(currentTime, caretPos, pointPos))
      predTime = currentTime
    } else {
      LOG.warn("Not same editor!")
    }
  }

  /**
    * Gets the hover request and shows it
    *
    * @param curTime   The current time
    * @param editorPos The editor position
    * @param point     The point at which to show the hint
    */
  private def requestAndShowDoc(curTime: Long, editorPos: LogicalPosition, point: Point): Unit = {
    val serverPos = DocumentUtils.logicalToLSPPos(editorPos)
    val request = requestManager.hover(new TextDocumentPositionParams(identifier, serverPos))
    if (request != null) {
      try {
        val hover = request.get(HOVER_TIMEOUT, TimeUnit.MILLISECONDS)
        if (hover != null) {
          val range = if (hover.getRange == null) new Range(DocumentUtils.logicalToLSPPos(editorPos), DocumentUtils.logicalToLSPPos(editorPos)) else hover.getRange
          val string = HoverHandler.getHoverString(hover)
          if (string != null && string != "") {
            if (isCtrlDown) {
              invokeLater(() => if (!editor.isDisposed) currentHint = createAndShowHint(editor, string, point, flags = HintManager.HIDE_BY_OTHER_HINT))
              val loc = requestDefinition(serverPos)
              if (loc != null) {
                invokeLater(() => {
                  if (!editor.isDisposed) {
                    val startOffset = DocumentUtils.LSPPosToOffset(editor, range.getStart)
                    val endOffset = DocumentUtils.LSPPosToOffset(editor, range.getEnd)
                    ctrlRange = CtrlRangeMarker(loc, editor, editor.getMarkupModel.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.HYPERLINK, editor.getColorsScheme.getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR), HighlighterTargetArea.EXACT_RANGE))
                  }
                })
              }
            } else {
              invokeLater(() => if (!editor.isDisposed) currentHint = createAndShowHint(editor, string, point))
            }
          } else {
            LOG.warn("Hover string returned is null for file " + identifier.getUri + " and pos (" + serverPos.getLine + ";" + serverPos.getCharacter + ")")
          }
        } else {
          LOG.warn("Hover is null for file " + identifier.getUri + " and pos (" + serverPos.getLine + ";" + serverPos.getCharacter + ")")
        }
      } catch {
        case e: TimeoutException =>
          LOG.warn(e)
      }
    }


  }

  /**
    * Returns the position of the definition given a position in the editor
    *
    * @param position The position
    * @return The location of the definition
    */
  private def requestDefinition(position: Position): Location = {
    val params = new TextDocumentPositionParams(identifier, position)
    val request = requestManager.definition(params)
    if (request != null) {
      try {
        val definition = request.get(DEFINITION_TIMEOUT, TimeUnit.MILLISECONDS).asScala
        if (definition != null && definition.nonEmpty) {
          definition.head
        } else {
          null
        }
      } catch {
        case e: TimeoutException =>
          LOG.warn(e)
          null
      }
    } else {
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
    if (editor == this.editor) {
      val serverPos = DocumentUtils.logicalToLSPPos(editor.offsetToLogicalPosition(offset))
      val request = requestManager.hover(new TextDocumentPositionParams(identifier, serverPos))
      if (request != null) {
        try {
          val response = request.get(HOVER_TIMEOUT, TimeUnit.MILLISECONDS)
          HoverHandler.getHoverString(response)
        } catch {
          case e: TimeoutException => LOG.warn(e)
            ""
        }
      } else {
        ""
      }
    } else {
      LOG.warn("Not same editor")
      ""
    }
  }

  /**
    * Handles the DocumentChanged events
    *
    * @param event The DocumentEvent
    */
  def documentChanged(event: DocumentEvent): Unit = {
    pool(() =>
      if (!editor.isDisposed) {
        if (event.getDocument == editor.getDocument) {
          predTime = System.nanoTime() //So that there are no hover events while typing
          changesParams.getTextDocument.setVersion({
            version += 1
            version - 1
          })
          syncKind match {
            case TextDocumentSyncKind.None =>
            case TextDocumentSyncKind.Incremental =>
              val changeEvent = changesParams.getContentChanges.get(0)
              val newText = event.getNewFragment
              val offset = event.getOffset
              val length = event.getNewLength
              val range = computableReadAction(() => new Range(DocumentUtils.offsetToLSPPos(editor, offset), DocumentUtils.offsetToLSPPos(editor, offset + length)))
              changeEvent.setRange(range)
              changeEvent.setRangeLength(length)
              changeEvent.setText(newText.toString)

            case TextDocumentSyncKind.Full =>
              changesParams.getContentChanges.get(0).setText(editor.getDocument.getText())
          }
          requestManager.didChange(changesParams)
        } else {
          LOG.error("Wrong document for the EditorEventManager")
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
        LOG.warn("Editor " + editor + " was already closed")
      }
    })
  }

  /**
    * Returns the completion suggestions given a position
    *
    * @param pos The LSP position
    * @return The suggestions
    */
  def completion(pos: Position): Iterable[_ <: LookupElement] = {
    val request = requestManager.completion(new TextDocumentPositionParams(identifier, pos))
    if (request != null) {
      try {
        val res = request.get(COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS)
        import scala.collection.JavaConverters._
        val completion /*: CompletionList | List[CompletionItem] */ = if (res.isLeft) res.getLeft.asScala else res.getRight

        /**
          * Creates a LookupElement given a CompletionItem
          *
          * @param item The CompletionItem
          * @return The corresponding LookupElement
          */
        def createLookupItem(item: CompletionItem): LookupElement = {
          val addTextEdits = item.getAdditionalTextEdits
          val command = item.getCommand
          val data = item.getData
          val detail = item.getDetail
          val doc = item.getDocumentation
          val filterText = item.getFilterText
          val insertText = item.getInsertText
          val insertFormat = item.getInsertTextFormat
          val kind = item.getKind
          val label = item.getLabel
          val textEdit = item.getTextEdit
          val sortText = item.getSortText
          val presentableText = if (label != null && label != "") label else if (insertText != null) insertText else ""
          val tailText = if (detail != null) detail else ""
          val iconProvider = GUIUtils.getIconProviderFor(wrapper.getServerDefinition)
          val icon = iconProvider.getCompletionIcon(kind)
          val lookupElementBuilder = LookupElementBuilder.create(if (insertText != null && insertText != "") insertText else label)
            .withPresentableText(presentableText).withTailText(tailText, true).withIcon(icon)
          /*            .withRenderer((element: LookupElement, presentation: LookupElementPresentation) => { //TODO later
                      presentation match {
                        case realPresentation: RealLookupElementPresentation =>
                          if (!realPresentation.hasEnoughSpaceFor(presentation.getItemText, presentation.isItemTextBold)) {
                          }
                      }
                    })*/
          if (kind == CompletionItemKind.Keyword) lookupElementBuilder.withBoldness(true)
          if (textEdit != null) {
            if (addTextEdits != null) {
              lookupElementBuilder.withInsertHandler((context: InsertionContext, item: LookupElement) => {
                context.commitDocument()
                invokeLater(() => applyEdit(edits = addTextEdits.asScala :+ textEdit, name = "Completion : " + label))
              })
            } else {
              lookupElementBuilder.withInsertHandler((context: InsertionContext, item: LookupElement) => {
                context.commitDocument()
                invokeLater(() => applyEdit(edits = Seq(textEdit), name = "Completion : " + label))
              })
            }
          } else if (addTextEdits != null) {
            lookupElementBuilder.withInsertHandler((context: InsertionContext, item: LookupElement) => {
              context.commitDocument()
              invokeLater(() => applyEdit(edits = addTextEdits.asScala, name = "Completion : " + label))
            })
          } else {
            lookupElementBuilder.withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT)
          }
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
      }
      catch {
        case e: TimeoutException =>
          LOG.warn(e)
          Iterable.empty
      }
    } else Iterable.empty
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
              if (edits != null) {
                invokeLater(() => applyEdit(edits = edits.asScala, name = "WaitUntil edits"))
              }
            } catch {
              case e: TimeoutException =>
                LOG.warn(e)
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
    * Returns the references given the position of the word to search for
    *
    * @param pos A logical position
    * @return An array of PsiReference
    */
  def references(pos: LogicalPosition): Array[PsiReference] = {
    /*    val lspPos = DocumentUtils.logicalToLSPPos(pos)
        val params = new ReferenceParams(new ReferenceContext(false))
        params.setPosition(lspPos)
        params.setTextDocument(identifier)
        val request = requestManager.references(params)
        if (request != null) {
          try {
            val res = request.get(REFERENCES_TIMEOUT, TimeUnit.MILLISECONDS)
            if (res != null) {
              ApplicationManager.getApplication.runReadAction(new Computable[Array[PsiReference]] {
                override def compute(): Array[PsiReference] = {
                  res.asScala.map(l => {
                    val start = l.getRange.getStart
                    val end = l.getRange.getEnd
                    val logicalStart = DocumentUtils.LSPPosToOffset(editor, start)
                    val logicalEnd = DocumentUtils.LSPPosToOffset(editor, end)
                    val name = editor.getDocument.getText(new TextRange(logicalStart, logicalEnd))
                    LSPPsiElement(name, editor.getProject, logicalStart, logicalEnd).getReference
                  }).toArray
                }
              })
            } else {
              Array()
            }
          } catch {
            case e: TimeoutException =>
              LOG.warn(e)
              Array()
          }
        } else Array.empty*/
    Array()
  }

  /**
    * Applies the diagnostics to the document
    *
    * @param diagnostics The diagnostics to apply from the server
    */
  def diagnostics(diagnostics: Iterable[Diagnostic]): Unit = {
    pool(() => {
      if (!editor.isDisposed) {
        invokeLater(() => {
          diagnosticsHighlights.foreach(highlight => editor.getMarkupModel.removeHighlighter(highlight.rangeHighlighter))
          diagnosticsHighlights.clear()
        })
        for (diagnostic <- diagnostics) {
          val code = diagnostic.getCode
          val message = diagnostic.getMessage
          val source = diagnostic.getSource
          val range = diagnostic.getRange
          val severity = diagnostic.getSeverity
          val (start, end) = computableReadAction(() => (DocumentUtils.LSPPosToOffset(editor, range.getStart), DocumentUtils.LSPPosToOffset(editor, range.getEnd)))

          val markupModel = editor.getMarkupModel
          val colorScheme = editor.getColorsScheme

          val (effectType, effectColor, layer) = severity match {
            case null => null
            case DiagnosticSeverity.Error => (EffectType.WAVE_UNDERSCORE, Color.RED, HighlighterLayer.ERROR)
            case DiagnosticSeverity.Warning => (EffectType.WAVE_UNDERSCORE, Color.YELLOW, HighlighterLayer.WARNING)
            case DiagnosticSeverity.Information => (EffectType.WAVE_UNDERSCORE, Color.GRAY, HighlighterLayer.WARNING)
            case DiagnosticSeverity.Hint => (EffectType.BOLD_DOTTED_LINE, Color.GRAY, HighlighterLayer.WARNING)
          }
          invokeLater(() => {
            if (!editor.isDisposed) {
              diagnosticsHighlights
                .add(DiagnosticRangeHighlighter(markupModel.addRangeHighlighter(start, end, layer,
                  new TextAttributes(colorScheme.getDefaultForeground, colorScheme.getDefaultBackground, effectColor, effectType, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE),
                  diagnostic))
            }
          })
        }
      }
    })
  }

  /**
    * Rename a symbol in the document
    *
    * @param renameTo The new name
    */
  //TODO documenthighlight => in place rename
  def rename(renameTo: String): Unit = {
    val servPos = DocumentUtils.logicalToLSPPos(editor.offsetToLogicalPosition(editor.getCaretModel.getCurrentCaret.getOffset))
    pool(() => {
      if (!editor.isDisposed) {
        val params = new RenameParams(identifier, servPos, renameTo)
        val request = requestManager.rename(params)
        if (request != null) request.thenAccept(res => WorkspaceEditHandler.applyEdit(res, "Rename to " + renameTo))
      }
    })
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
        if (request != null) request.thenAccept(formatting =>
          invokeLater(() =>
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
    if (version >= this.version) {
      val document = editor.getDocument
      if (document.isWritable) {
        val runnable = new Runnable {
          override def run(): Unit = {
            if (!editor.isDisposed) {
              edits.foreach(edit => {
                val text = edit.getNewText
                val range = edit.getRange
                val start = DocumentUtils.LSPPosToOffset(editor, range.getStart)
                val end = DocumentUtils.LSPPosToOffset(editor, range.getEnd)
                if (text == "" || text == null) {
                  document.deleteString(start, end)
                } else if (end - start <= 0) {
                  document.insertString(start, text)
                } else {
                  document.replaceString(start, end, text)
                }
              })
              FileDocumentManager.getInstance().saveDocument(document)
            }
          }
        }
        writeAction(() => {
          CommandProcessor.getInstance().executeCommand(editor.getProject, runnable, name, "LSPPlugin", document)
          if (closeAfter) {
            FileEditorManager.getInstance(editor.getProject)
              .closeFile(PsiDocumentManager.getInstance(editor.getProject).getPsiFile(editor.getDocument).getVirtualFile)
          }
        })
      } else {
        LOG.warn("Document is not writable")
      }
      true
    } else {
      LOG.warn("Version " + version + " is older than " + this.version)
      false
    }
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
        val start = selectionModel.getSelectionStart
        val end = selectionModel.getSelectionEnd
        val startingPos = DocumentUtils.offsetToLSPPos(editor, start)
        val endPos = DocumentUtils.offsetToLSPPos(editor, end)
        params.setRange(new Range(startingPos, endPos))
        val options = new FormattingOptions() //TODO
        params.setOptions(options)
        val request = requestManager.rangeFormatting(params)
        if (request != null) request.thenAccept(formatting => invokeLater(() => applyEdit(edits = formatting.asScala, name = "Reformat selection")))
      }
    })
  }

  /**
    * Calls onTypeFormatting or signatureHelp if the character typed was a trigger character
    *
    * @param c The character just typed
    */
  def characterTyped(c: Char): Unit = {
    if (completionTriggers.contains(c.toString)) {
      //completion(DocumentUtils.offsetToLSPPos(editor,editor.getCaretModel.getCurrentCaret.getOffset))
    } else if (signatureTriggers.contains(c.toString)) {
      signatureHelp()
    } else if (onTypeFormattingTriggers.contains(c.toString)) {
      onTypeFormatting(c.toString)
    }
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
        params.setPosition(computableReadAction(() => DocumentUtils.logicalToLSPPos(editor.getCaretModel.getCurrentCaret.getLogicalPosition)))
        params.setTextDocument(identifier)
        params.setOptions(new FormattingOptions())
        val future = requestManager.onTypeFormatting(params)
        if (future != null) {
          try {
            val edits = future.get(FORMATTING_TIMEOUT, TimeUnit.MILLISECONDS)
            invokeLater(() => applyEdit(edits = edits.asScala, name = "On type formatting"))
          } catch {
            case e: TimeoutException => LOG.warn(e)
          }
        }
      }
    })
  }

  /**
    * Calls signatureHelp at the current editor caret position
    */
  def signatureHelp(): Unit = {
    val lPos = editor.getCaretModel.getCurrentCaret.getLogicalPosition
    val point = editor.logicalPositionToXY(lPos)
    val params = new TextDocumentPositionParams(identifier, DocumentUtils.logicalToLSPPos(lPos))
    pool(() => {
      if (!editor.isDisposed) {
        val future = requestManager.signatureHelp(params)
        if (future != null) {
          try {
            val signature = future.get(SIGNATURE_TIMEOUT, TimeUnit.MILLISECONDS)
            if (signature != null) {
              val signatures = signature.getSignatures.asScala
              if (signatures != null && signatures.nonEmpty) {
                val activeSignatureIndex = signature.getActiveSignature
                val activeParameterIndex = signature.getActiveParameter
                val activeParameter = signatures(activeSignatureIndex).getParameters.get(activeParameterIndex).getLabel
                val builder = StringBuilder.newBuilder
                builder.append("<html>")
                signatures.take(activeSignatureIndex).foreach(sig => builder.append(sig.getLabel).append("<br>"))
                builder.append("<i>").append(signatures(activeSignatureIndex).getLabel
                  .replace(activeParameter, "<b><font color=\"yellow\">" + activeParameter + "</font></b>")).append("</i>")
                signatures.drop(activeSignatureIndex + 1).foreach(sig => builder.append("<br>").append(sig.getLabel))
                builder.append("</html>")
                invokeLater(() => currentHint = createAndShowHint(editor, builder.toString(), point, HintManager.UNDER, HintManager.HIDE_BY_OTHER_HINT))
              }
            }
          } catch {
            case e: TimeoutException => LOG.warn(e)
          }
        }
      }
    })
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
          edits.foreach(edit => {
            val text = edit.getNewText
            val range = edit.getRange
            val start = DocumentUtils.LSPPosToOffset(editor, range.getStart)
            val end = DocumentUtils.LSPPosToOffset(editor, range.getEnd)
            if (text == "" || text == null) {
              document.deleteString(start, end)
            } else if (end - start <= 0) {
              document.insertString(start, text)
            } else {
              document.replaceString(start, end, text)
            }
          })
          saveDocument()
        }
      } else {
        LOG.warn("Document is not writable")
        null
      }
    } else {
      LOG.warn("Version " + version + " is older than " + this.version)
      null
    }
  }

  private def saveDocument(): Unit = {
    invokeLater(() => writeAction(() => FileDocumentManager.getInstance().saveDocument(editor.getDocument)))
  }

  /**
    * Retrieves the commands needed to apply a CodeAction
    *
    * @param element The element which needs the CodeAction
    * @return The list of commands, or null if none are given / the request times out
    */
  def codeAction(element: LSPPsiElement): Iterable[Command] = {
    val params = new CodeActionParams()
    params.setTextDocument(identifier)
    params.setRange(computableReadAction(() => new Range(DocumentUtils.offsetToLSPPos(editor, element.start), DocumentUtils.offsetToLSPPos(editor, element.end))))
    val context = new CodeActionContext(diagnosticsHighlights.map(_.diagnostic).toList.asJava)
    params.setContext(context)
    val future = requestManager.codeAction(params)
    if (future != null) {
      try {
        future.get(CODEACTION_TIMEOUT, TimeUnit.MILLISECONDS).asScala
      } catch {
        case e: TimeoutException =>
          LOG.warn(e)
          null
      }
    } else {
      null
    }
  }
}
