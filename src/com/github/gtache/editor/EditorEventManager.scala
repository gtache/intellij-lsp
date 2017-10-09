package com.github.gtache.editor

import java.util.concurrent.TimeUnit
import java.util.{Timer, TimerTask}

import com.github.gtache.Utils
import com.github.gtache.client.RequestManager
import com.github.gtache.requests.HoverHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.{EditorMouseEvent, EditorMouseMotionListener}
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.ui.popup.{JBPopupFactory, JBPopupListener, LightweightWindowEvent}
import org.eclipse.lsp4j._

/**
  * Class handling events related to an Editor (a Document)
  *
  * @param editor              The "watched" editor
  * @param mouseMotionListener A MouseMotionListener listening to the editor
  * @param requestManager      The related RequestManager, connected to the right LanguageServer
  * @param syncKind            The type of synchronisation (unused)
  */
class EditorEventManager(val editor: Editor, val mouseMotionListener: EditorMouseMotionListener, val requestManager: RequestManager, val syncKind: TextDocumentSyncKind = TextDocumentSyncKind.Full) {
  {
    editor.addEditorMouseMotionListener(mouseMotionListener)
  }
  private val RESPONSE_TIME: Int = 1000 //Time in millis to get a response
  private val HOVER_TIME_THRES: Long = 2000000000L //2 sec
  private val identifier: TextDocumentIdentifier = new TextDocumentIdentifier(Utils.editorToURIString(editor))
  private val LOG: Logger = Logger.getInstance(classOf[EditorEventManager])
  private var predTime: Long = -1L
  private var isOpen: Boolean = true
  @volatile private var isPopupOpen: Boolean = false
  private val hoverThread = new Timer("Hover", true)
  private val scheduleThres = 100000000 //Time before the Timer is scheduled
  private val POPUP_THRES = HOVER_TIME_THRES / 1000000 + 200


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
    * Handles the mouseMoved event : If the mouse doesn't move for 1s, an Hover request will be sent
    *
    * @param e the event
    */
  def mouseMoved(e: EditorMouseEvent): Unit = {
    val curTime = System.nanoTime()
    if (predTime == (-1L)) {
      predTime = curTime
    } else {
      if (!isPopupOpen) {
        val editorPos = getPos(e)
        if (curTime - predTime > scheduleThres) {
          hoverThread.schedule(new TimerTask {
            override def run(): Unit = {
              val curTime = System.nanoTime()
              if (curTime - predTime > HOVER_TIME_THRES) {
                isPopupOpen = true
                val serverPos = Utils.logicalToLspPos(editorPos)
                val response = requestManager.hover(new TextDocumentPositionParams(identifier, serverPos))
                val hover = response.get(RESPONSE_TIME, TimeUnit.MILLISECONDS)
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
                  LOG.warn("String returned is null for file " + identifier.getUri + " and pos (" + serverPos.getLine + ";" + serverPos.getCharacter + ")")
                }


              }
            }
          }, POPUP_THRES)
        }
      }
      predTime = curTime
    }
  }

  //TODO
  def documentChanged(): Unit = {
    requestManager.didChange(new DidChangeTextDocumentParams())
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

}
