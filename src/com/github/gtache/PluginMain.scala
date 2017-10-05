package com.github.gtache

import java.io.File

import com.github.gtache.client.{LanguageServerDefinition, LanguageServerWrapper}
import com.github.gtache.editor.{EditorListener, VFSListener}
import com.github.gtache.settings.LSPState
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.{VirtualFile, VirtualFileManager}

import scala.collection.immutable.HashMap
import scala.collection.mutable

object PluginMain {
  private val dottyM = "dotty.tools.languageserver.Main"
  private val LOG: Logger = Logger.getInstance(classOf[PluginMain])
  private val extToLanguageWrapper: mutable.Map[String, LanguageServerWrapper] = mutable.HashMap()
  private val uriToLanguageWrapper: mutable.Map[String, LanguageServerWrapper] = mutable.HashMap()
  private var extToServLoc: Map[String, String] = HashMap()

  def setExtToServ(newExt: java.util.Map[String, String]): Unit = {
    import scala.collection.JavaConverters._
    setExtToServ(newExt.asScala)
  }

  def setExtToServ(newExt: collection.Map[String, String]): Unit = extToServLoc = newExt.toMap

  def getExtToServLoc: Map[String, String] = extToServLoc

  def getExtToServLocJava: java.util.Map[String, String] = {
    import scala.collection.JavaConverters._
    extToServLoc.asJava
  }


  def editorOpened(editor: Editor): Unit = {
    val file: VirtualFile = FileDocumentManager.getInstance.getFile(editor.getDocument)
    if (file != null) {
      val ext: String = file.getExtension
      LOG.info("File " + file.getName + " opened.")
      extToServLoc.get(ext).foreach(s => {
        var wrapper = extToLanguageWrapper.get(ext).orNull
        if (wrapper == null) {
          LOG.info("Creating wrapper for ext " + ext)
          //TODO this line is for dotty
          wrapper = new LanguageServerWrapper(new LanguageServerDefinition(ext), Seq("coursier.bat", "launch", new File(s).getName, "-M", dottyM, "--", "-stdio"), new File(s).getParent)
          extToLanguageWrapper.put(ext, wrapper)
        }
        LOG.info("Adding file " + file.getName)
        wrapper.connect(editor)
        uriToLanguageWrapper.put(Utils.editorToURIString(editor), wrapper)
      })
    } else {
      LOG.warn("file for editor " + editor.getDocument.getText + " is null?")
    }
  }

  def fileChanged(file: VirtualFile): Unit = {
    val uri: String = Utils.VFSToURIString(file)
    uriToLanguageWrapper.get(uri).foreach(l => {
      LOG.info("File saved : " + uri)
      l.getManagerFor(uri).documentSaved()
    })
  }

  def fileMoved(file: VirtualFile): Unit = {

  }

  def fileDeleted(file: VirtualFile): Unit = {

  }

  def fileRenamed(oldV: String, newV: String): Unit = {

  }

  def fileCreated(file: VirtualFile): Unit = {

  }


  def editorClosed(editor: Editor): Unit = {
    val file: VirtualFile = FileDocumentManager.getInstance.getFile(editor.getDocument)
    if (file != null) {
      val ext: String = file.getExtension
      LOG.info("File " + file.getName + " closed.")
      extToServLoc.get(ext) match {
        case Some(string) =>
          val uri = Utils.editorToURIString(editor)
          uriToLanguageWrapper.get(uri).foreach(l => {
            LOG.info("Disconnecting " + uri)
            l.disconnect(uri)
          })
          uriToLanguageWrapper.remove(uri)
        case None =>
          LOG.info("Closing LSP-unsupported file with extension " + ext)
      }
    } else {
      LOG.warn("File for document " + editor.getDocument.getText + " is null")
    }
  }
}

class PluginMain extends ApplicationComponent {

  import com.github.gtache.PluginMain._

  override val getComponentName: String = "PluginMain"

  override def initComponent(): Unit = {
    LSPState.getInstance.getState //Need that to trigger loadState

    EditorFactory.getInstance.addEditorFactoryListener(new EditorListener)
    VirtualFileManager.getInstance().addVirtualFileListener(VFSListener)
    LOG.info("PluginMain init finished")
  }
}
