package com.github.gtache.editor

import java.awt.{Color, Font}
import java.util.concurrent.{TimeUnit, TimeoutException}
import java.util.{Collections, Timer, TimerTask}

import com.github.gtache.Utils
import com.github.gtache.client.{LanguageServerWrapper, RequestManager}
import com.github.gtache.requests.HoverHandler
import com.intellij.codeInsight.lookup.{AutoCompletionPolicy, LookupElement, LookupElementBuilder}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event._
import com.intellij.openapi.editor.markup.{HighlighterLayer, HighlighterTargetArea, RangeHighlighter, TextAttributes}
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.ui.popup.{JBPopupFactory, JBPopupListener, LightweightWindowEvent}
import org.eclipse.lsp4j._

import scala.collection.mutable

object EditorEventManager {
  private val HOVER_TIME_THRES: Long = 2000000000L //2 sec
  private val SCHEDULE_THRES = 10000000 //Time before the Timer is scheduled
  private val POPUP_THRES = HOVER_TIME_THRES / 1000000 + 200
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
class EditorEventManager(val editor: Editor, val mouseListener: EditorMouseListener, val mouseMotionListener: EditorMouseMotionListener, val documentListener: DocumentListener, val selectionListener: SelectionListener, val requestManager: RequestManager, val syncKind: TextDocumentSyncKind = TextDocumentSyncKind.Full, val wrapper: LanguageServerWrapper) {


  import com.github.gtache.Timeout._
  import com.github.gtache.editor.EditorEventManager._

  private val identifier: TextDocumentIdentifier = new TextDocumentIdentifier(Utils.editorToURIString(editor))
  private val LOG: Logger = Logger.getInstance(classOf[EditorEventManager])
  private val hoverThread = new Timer("Hover", true)
  private val versionStream = Stream.from(0).iterator
  private val changesParams = new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(), Collections.singletonList(new TextDocumentContentChangeEvent()))
  private val currentHighlights: mutable.Set[RangeHighlighter] = mutable.HashSet()
  private var predTime: Long = -1L
  private var isOpen: Boolean = true
  @volatile private var isPopupOpen: Boolean = false
  private var mouseInEditor: Boolean = true

  changesParams.getTextDocument.setUri(Utils.editorToURIString(editor))
  editor.addEditorMouseListener(mouseListener)
  editor.addEditorMouseMotionListener(mouseMotionListener)
  editor.getDocument.addDocumentListener(documentListener)
  editor.getSelectionModel.addSelectionListener(selectionListener)
  requestManager.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(Utils.editorToURIString(editor), wrapper.serverDefinition.id, versionStream.next, editor.getDocument.getText)))

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
    * Handles the mouseMoved event : If the mouse doesn't move for 2s, an Hover request will be sent
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
          val editorPos = getPos(e)
          if (curTime - predTime > SCHEDULE_THRES) {
            hoverThread.schedule(new TimerTask {
              override def run(): Unit = {
                val curTime = System.nanoTime()
                if (curTime - predTime > HOVER_TIME_THRES && mouseInEditor) {
                  isPopupOpen = true
                  val serverPos = Utils.logicalToLSPPos(editorPos)
                  try {
                    val response = requestManager.hover(new TextDocumentPositionParams(identifier, serverPos))
                    val hover = response.get(HOVER_TIMEOUT, TimeUnit.MILLISECONDS)
                    val range = hover.getRange
                    val string = HoverHandler.getHoverString(hover)
                    if (string != null) {
                      ApplicationManager.getApplication.invokeLater(() => {
                        val popup = JBPopupFactory.getInstance().createMessage(string)
                        popup.addListener(new JBPopupListener {
                          override def onClosed(lightweightWindowEvent: LightweightWindowEvent): Unit = {
                            isPopupOpen = false
                            predTime = curTime
                          }

                          override def beforeShown(lightweightWindowEvent: LightweightWindowEvent): Unit = {}
                        })
                        popup.showInScreenCoordinates(editor.getComponent, e.getMouseEvent.getLocationOnScreen)
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
              }
            }, POPUP_THRES)
          }
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

  /**
    * Handles the DocumentChanged events
    *
    * @param event The DocumentEvent
    */
  def documentChanged(event: DocumentEvent): Unit = {
    if (event.getDocument == editor.getDocument) {
      predTime = System.nanoTime() //So that there are no hover events while typing
      changesParams.getTextDocument.setVersion(versionStream.next())
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
  def willSave(): Unit = {
    requestManager.willSave(new WillSaveTextDocumentParams(identifier, TextDocumentSaveReason.Manual))
  }

}
