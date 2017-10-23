package com.github.gtache.editor

import java.awt.{Color, Font, Point}
import java.util.concurrent.{TimeUnit, TimeoutException}
import java.util.{Collections, Timer, TimerTask}

import com.github.gtache.client.{LanguageServerWrapperImpl, RequestManager}
import com.github.gtache.requests.HoverHandler
import com.github.gtache.{LSPPsiElement, Utils}
import com.intellij.codeInsight.lookup.{AutoCompletionPolicy, LookupElement, LookupElementBuilder}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event._
import com.intellij.openapi.editor.markup.{HighlighterLayer, HighlighterTargetArea, RangeHighlighter, TextAttributes}
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.ui.popup.{Balloon, JBPopupFactory, JBPopupListener, LightweightWindowEvent}
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.ui.awt.RelativePoint
import org.eclipse.lsp4j._

import scala.collection.mutable

object EditorEventManager {
  private val HOVER_TIME_THRES: Long = 2000000000L //2 sec
  private val SCHEDULE_THRES = 10000000 //Time before the Timer is scheduled
  private val POPUP_THRES = HOVER_TIME_THRES / 1000000 + 50
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
  * @param syncKind            The type of synchronization
  * @param wrapper             The corresponding LanguageServerWrapper
  */
class EditorEventManager(val editor: Editor, val mouseListener: EditorMouseListener, val mouseMotionListener: EditorMouseMotionListener, val documentListener: DocumentListener, val selectionListener: SelectionListener, val requestManager: RequestManager, val syncKind: TextDocumentSyncKind = TextDocumentSyncKind.Full, val wrapper: LanguageServerWrapperImpl) {


  import com.github.gtache.Timeout._
  import com.github.gtache.editor.EditorEventManager._

  private val identifier: TextDocumentIdentifier = new TextDocumentIdentifier(Utils.editorToURIString(editor))
  private val LOG: Logger = Logger.getInstance(classOf[EditorEventManager])
  private val hoverThread = new Timer("Hover", true)
  private val changesParams = new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(), Collections.singletonList(new TextDocumentContentChangeEvent()))
  private val currentHighlights: mutable.Set[RangeHighlighter] = mutable.HashSet()
  private var version: Int = -1
  private var predTime: Long = -1L
  private var isOpen: Boolean = true
  @volatile private var isPopupOpen: Boolean = false
  private var mouseInEditor: Boolean = true
  @volatile private var currentPopup: Balloon = _

  changesParams.getTextDocument.setUri(Utils.editorToURIString(editor))
  editor.addEditorMouseListener(mouseListener)
  editor.addEditorMouseMotionListener(mouseMotionListener)
  editor.getDocument.addDocumentListener(documentListener)
  editor.getSelectionModel.addSelectionListener(selectionListener)
  requestManager.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(Utils.editorToURIString(editor), wrapper.serverDefinition.id, {
    version += 1;
    version
  }, editor.getDocument.getText)))

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
    * Manages the change of selected text in the editor
    *
    * @param e The selection event
    */
  def selectionChanged(e: SelectionEvent): Unit = {
    if (e.getEditor == editor) {
      currentHighlights.foreach(h => editor.getMarkupModel.removeHighlighter(h))
      currentHighlights.clear()
      if (editor.getSelectionModel.hasSelection) {
        val ideRange = e.getNewRange
        val LSPPos = Utils.offsetToLSPPos(editor, ideRange.getStartOffset)
        val request = requestManager.documentHighlight(new TextDocumentPositionParams(identifier, LSPPos))
        ApplicationManager.getApplication.executeOnPooledThread(new Runnable {
          override def run(): Unit = {
            import scala.collection.JavaConverters._
            try {
              val resp = request.get(DOC_HIGHLIGHT_TIMEOUT, TimeUnit.MILLISECONDS).asScala
              ApplicationManager.getApplication.invokeLater(() => resp.foreach(dh => {
                val range = dh.getRange
                val kind = dh.getKind
                val startOffset = Utils.LSPPosToOffset(editor, range.getStart)
                val endOffset = Utils.LSPPosToOffset(editor, range.getEnd)
                val colorScheme = editor.getColorsScheme
                //TODO hardcoded
                val highlight = editor.getMarkupModel.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION, new TextAttributes(colorScheme.getDefaultForeground, new Color(54, 64, 55), null, null, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE)
                currentHighlights.add(highlight)
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

  /**
    * Will show documentation if the mosue doesn't move for a given time (Hover)
    *
    * @param e the event
    */
  def mouseMoved(e: EditorMouseEvent): Unit = {
    if (e.getEditor == editor) {
      val curTime = System.nanoTime()
      if (predTime == (-1L)) {
        predTime = curTime
      } else {
        if (!isPopupOpen) {
          scheduleDocumentation(curTime, getPos(e), e.getMouseEvent.getPoint)
        } else if (currentPopup != null) {
          currentPopup.hide()
          currentPopup = null
        }
        predTime = curTime
      }
    } else {
      LOG.error("Wrong editor for EditorEventManager")
    }
  }

  private def getPos(e: EditorMouseEvent): LogicalPosition = {
    val mousePos = e.getMouseEvent.getPoint
    var editorPos = editor.xyToLogicalPosition(mousePos)
    val doc = e.getEditor.getDocument
    val maxLines = doc.getLineCount
    if (editorPos.line >= maxLines) {
      editorPos = new LogicalPosition(maxLines - 1, editorPos.column)
    }
    val minY = doc.getLineStartOffset(editorPos.line) - (if (editorPos.line > 0) doc.getLineEndOffset(editorPos.line - 1) else 0)
    val maxY = doc.getLineEndOffset(editorPos.line) - (if (editorPos.line > 0) doc.getLineEndOffset(editorPos.line - 1) else 0)
    if (editorPos.column < minY) {
      editorPos = new LogicalPosition(editorPos.line, minY)
    } else if (editorPos.column > maxY) {
      editorPos = new LogicalPosition(editorPos.line, maxY)
    }
    editorPos
  }

  private def scheduleDocumentation(time: Long, editorPos: LogicalPosition, point: Point): Unit = {
    if (time - predTime > SCHEDULE_THRES) {
      hoverThread.schedule(new TimerTask {
        override def run(): Unit = {
          val curTime = System.nanoTime()
          if (curTime - predTime > HOVER_TIME_THRES && mouseInEditor && editor.getContentComponent.hasFocus && !isPopupOpen) {
            requestAndShowDoc(curTime, editorPos, point)
          }
        }
      }, POPUP_THRES)
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
      ApplicationManager.getApplication.executeOnPooledThread(new Runnable {
        override def run(): Unit = requestAndShowDoc(currentTime, caretPos, pointPos)
      })
      predTime = currentTime
    } else {
      LOG.warn("Not same editor!")
    }
  }

  private def requestAndShowDoc(curTime: Long, editorPos: LogicalPosition, point: Point): Unit = {
    isPopupOpen = true
    val serverPos = Utils.logicalToLSPPos(editorPos)
    try {
      val response = requestManager.hover(new TextDocumentPositionParams(identifier, serverPos))
      val hover = response.get(HOVER_TIMEOUT, TimeUnit.MILLISECONDS)
      val range = hover.getRange
      val string = HoverHandler.getHoverString(hover)
      if (string != null) {
        ApplicationManager.getApplication.invokeLater(() => {
          val popupBuilder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(string, com.intellij.openapi.ui.MessageType.INFO, null)
          popupBuilder.setHideOnKeyOutside(true).setHideOnAction(true).setHideOnClickOutside(true).setHideOnCloseClick(true).setHideOnLinkClick(true).setHideOnFrameResize(true)
          currentPopup = popupBuilder.createBalloon()
          currentPopup.addListener(new JBPopupListener {
            override def onClosed(lightweightWindowEvent: LightweightWindowEvent): Unit = {
              isPopupOpen = false
              predTime = curTime
            }

            override def beforeShown(lightweightWindowEvent: LightweightWindowEvent): Unit = {}
          })
          currentPopup.show(new RelativePoint(editor.getContentComponent, point), Balloon.Position.above)
        })
      } else {
        isPopupOpen = false
        LOG.warn("Hover string returned is null for file " + identifier.getUri + " and pos (" + serverPos.getLine + ";" + serverPos.getCharacter + ")")
      }
    } catch {
      case e: TimeoutException =>
        isPopupOpen = false
        LOG.warn(e)
    }


  }

  /**
    * Requests the Hover information, synchronously
    *
    * @param editor The editor
    * @param offset The offset in the editor
    * @return The information
    */
  def requestDoc(editor: Editor, offset: Int): String = {
    if (editor == this.editor) {
      val serverPos = Utils.logicalToLSPPos(editor.offsetToLogicalPosition(offset))
      try {
        val response = requestManager.hover(new TextDocumentPositionParams(identifier, serverPos)).get(HOVER_TIMEOUT, TimeUnit.MILLISECONDS)
        HoverHandler.getHoverString(response)
      } catch {
        case e: TimeoutException => LOG.warn(e)
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
    if (event.getDocument == editor.getDocument) {
      predTime = System.nanoTime() //So that there are no hover events while typing
      changesParams.getTextDocument.setVersion({
        version += 1;
        version
      })
      syncKind match {
        case TextDocumentSyncKind.None =>
        case TextDocumentSyncKind.Incremental =>
          val changeEvent = changesParams.getContentChanges.get(0)
          val newText = event.getNewFragment
          val offset = event.getOffset
          val length = event.getNewLength
          val range = new Range(Utils.offsetToLSPPos(editor, offset), Utils.offsetToLSPPos(editor, offset + length))
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
  }

  /**
    * Notifies the server that the corresponding document has been saved
    */
  def documentSaved(): Unit = {
    val params: DidSaveTextDocumentParams = new DidSaveTextDocumentParams(Utils.editorToLSPIdentifier(editor), editor.getDocument.getText)
    requestManager.didSave(params)
  }

  /**
    * Notifies the server that the corresponding document has been closed
    */
  def documentClosed(): Unit = {
    if (isOpen) {
      requestManager.didClose(new DidCloseTextDocumentParams(Utils.editorToLSPIdentifier(editor)))
      isOpen = false
    } else {
      LOG.warn("Editor " + editor + " was already closed")
    }
  }

  /**
    * Returns the completion suggestions given a position
    *
    * @param pos The LSP position
    * @return The suggestions
    */
  def completion(pos: Position): Iterable[_ <: LookupElement] = {
    val future = requestManager.completion(new TextDocumentPositionParams(identifier, pos))
    try {
      val res = future.get(COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS)
      import scala.collection.JavaConverters._
      val completion /*: CompletionList | List[CompletionItem] */ = if (res.isLeft) res.getLeft.asScala else res.getRight
      completion match {
        case c: CompletionList => c.getItems.asScala.map(item => LookupElementBuilder.create(item.getLabel).withPresentableText(item.getLabel).withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT))
        case l: List[CompletionItem@unchecked] => l.map(item => {
          LookupElementBuilder.create(item.getLabel).withPresentableText(item.getLabel).withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT)
        })
      }
    } catch {
      case e: TimeoutException =>
        LOG.warn(e)
        Iterable.empty
    }
  }

  //TODO Manual
  /**
    * Indicates that the document will be saved
    */
  def willSave(): Unit = {
    requestManager.willSave(new WillSaveTextDocumentParams(identifier, TextDocumentSaveReason.Manual))
  }

  /**
    * Returns the references given the position of the word to search for
    *
    * @param pos A logical position
    * @return An array of PsiReference
    */
  def references(pos: LogicalPosition): Array[PsiReference] = {
    LOG.info("References")
    val lspPos = Utils.logicalToLSPPos(pos)
    val params = new ReferenceParams(new ReferenceContext(false))
    params.setPosition(lspPos)
    params.setTextDocument(identifier)
    val references = requestManager.references(params)
    try {
      val res = references.get(REFERENCES_TIMEOUT, TimeUnit.MILLISECONDS)
      import scala.collection.JavaConverters._
      res.asScala.map(l => {
        val start = l.getRange.getStart
        val end = l.getRange.getEnd
        val logicalStart = Utils.LSPPosToOffset(editor, start)
        val logicalEnd = Utils.LSPPosToOffset(editor, end)
        val name = editor.getDocument.getText(new TextRange(logicalStart, logicalEnd))
        LSPPsiElement(name, editor.getProject, logicalStart, logicalEnd).getReference
      }).toArray
    } catch {
      case e: TimeoutException =>
        LOG.warn(e)
        Array()
    }
  }

  /**
    * Applies the edits given a version
    *
    * @param version The version of the changes ; If it is lower than the current version, they are discarded
    * @param edits   The edits
    * @return True if the edits have been applied, false otherwise
    */
  def applyEdit(version: Int = -1, edits: List[TextEdit]): Boolean = {
    if (version >= this.version) {
      edits.foreach(edit => {
        val text = edit.getNewText
        val range = edit.getRange
        val start = Utils.LSPPosToOffset(editor, range.getStart)
        val end = Utils.LSPPosToOffset(editor, range.getEnd)
        val document = editor.getDocument
        if (text == "") {
          document.deleteString(start, end)
        } else if (end - start <= 0) {
          document.insertString(start, text)
        } else {
          document.replaceString(start, end, text)
        }
      })
      true
    } else {
      false
    }
  }

}
