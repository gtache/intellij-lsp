package com.github.gtache.editor

import java.util.concurrent.TimeUnit

import com.github.gtache.Utils
import com.github.gtache.client.RequestManager
import com.github.gtache.requests.HoverHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.{EditorMouseEvent, EditorMouseMotionListener}
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
  private val RESPONSE_TIME: Int = 1000 //Time in millis
  private val HOVER_TIME_THRES: Long = 1000000000L //1 sec
  private val identifier: TextDocumentIdentifier = new TextDocumentIdentifier(Utils.editorToURIString(editor))
  private val LOG: Logger = Logger.getInstance(classOf[EditorEventManager])
  private var predTime: Long = -1L
  private var isOpen: Boolean = true
  private var isPopupOpen: Boolean = false

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
      if (curTime - predTime >= HOVER_TIME_THRES && !isPopupOpen) {
        val mousePos = e.getMouseEvent.getPoint
        val editorPos = editor.xyToLogicalPosition(mousePos)
        val serverPos = Utils.logicalToLspPos(editorPos)
        val response = requestManager.hover(new TextDocumentPositionParams(identifier, serverPos))
        ApplicationManager.getApplication.executeOnPooledThread(new Runnable {
          override def run(): Unit = {
            val hover = response.get(RESPONSE_TIME, TimeUnit.MILLISECONDS)
            val range = hover.getRange
            val string = HoverHandler.getHoverString(hover)
            if (string != null) {
              ApplicationManager.getApplication.invokeLater(() => {
                val popup = JBPopupFactory.getInstance().createMessage(string)
                popup.addListener(new JBPopupListener {
                  override def onClosed(lightweightWindowEvent: LightweightWindowEvent): Unit = {
                    isPopupOpen = false
                  }

                  override def beforeShown(lightweightWindowEvent: LightweightWindowEvent): Unit = {}
                })
                popup.showInScreenCoordinates(editor.getComponent, e.getMouseEvent.getLocationOnScreen)
                isPopupOpen = true
              })
            } else {
              LOG.warn("String returned is null for file " + identifier.getUri + " and pos (" + serverPos.getLine + ";" + serverPos.getCharacter + ")")
            }
          }
        })
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
