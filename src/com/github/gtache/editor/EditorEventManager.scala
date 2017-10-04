package com.github.gtache.editor

import java.util.concurrent.TimeUnit

import com.github.gtache.Utils
import com.github.gtache.client.RequestManager
import com.github.gtache.requests.HoverHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.{EditorMouseEvent, EditorMouseMotionListener}
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.eclipse.lsp4j._

class EditorEventManager(val editor: Editor, val mouseMotionListener: EditorMouseMotionListener, val requestManager: RequestManager, val syncKind: TextDocumentSyncKind = TextDocumentSyncKind.Full) {

  private val RESPONSE_TIME: Int = 500 //Time in millis
  private val HOVER_TIME_THRES: Long = 1000000000L //1 sec
  private val identifier: TextDocumentIdentifier = new TextDocumentIdentifier(Utils.editorToURIString(editor))
  private val LOG: Logger = Logger.getInstance(classOf[EditorEventManager])
  private var predTime: Long = -1L
  private var isOpen: Boolean = true

  def mouseMoved(e: EditorMouseEvent): Unit = {
    val curTime = System.nanoTime()
    if (predTime == (-1L)) {
      predTime = curTime
    } else {
      if (curTime - predTime >= HOVER_TIME_THRES) {
        val mousePos = e.getMouseEvent.getPoint
        val editorPos = editor.xyToLogicalPosition(mousePos)
        val serverPos = Utils.logicalToLspPos(editorPos)
        val response = requestManager.hover(new TextDocumentPositionParams(identifier, serverPos))
        ApplicationManager.getApplication.executeOnPooledThread(new Runnable {
          override def run(): Unit = {
            val hover = response.get(RESPONSE_TIME, TimeUnit.MILLISECONDS)
            val range = hover.getRange
            val string = HoverHandler.getHoverString(hover)
            val popup = JBPopupFactory.getInstance().createMessage(string)
            popup.showInBestPositionFor(editor)
          }
        })
      }
      predTime = curTime
    }
  }

  def documentSaved(): Unit = {
    val params: DidSaveTextDocumentParams = new DidSaveTextDocumentParams(Utils.editorToLSPIdentifier(editor), editor.getDocument.getText)
    requestManager.didSave(params)
  }

  def documentClosed(): Unit = {
    if (isOpen) {
      requestManager.didClose(new DidCloseTextDocumentParams(Utils.editorToLSPIdentifier(editor)))
      isOpen = false
    } else {
      LOG.warn("Editor " + editor + " was already closed")
    }
  }

}
