package com.github.gtache

import com.github.gtache.client.{LanguageServerDefinition, LanguageServerWrapper}
import com.github.gtache.editor.listeners.{EditorListener, VFSListener}
import com.github.gtache.settings.LSPState
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.{VirtualFile, VirtualFileManager}
import org.eclipse.lsp4j.Position

import scala.collection.immutable.HashMap
import scala.collection.mutable

/**
  * The main class of the plugin
  */
object PluginMain {
  private val dottyM = "dotty.tools.languageserver.Main" //TODO
  private val LOG: Logger = Logger.getInstance(classOf[PluginMain])
  private val extToLanguageWrapper: mutable.Map[String, LanguageServerWrapper] = mutable.HashMap()
  private val uriToLanguageWrapper: mutable.Map[String, LanguageServerWrapper] = mutable.HashMap()
  private var extToServLoc: Map[String, String] = HashMap()

  /**
    * Sets the extensions->languageServer mapping
    *
    * @param newExt a Java Map
    */
  def setExtToServ(newExt: java.util.Map[String, String]): Unit = {
    import scala.collection.JavaConverters._
    setExtToServ(newExt.asScala)
  }

  /**
    * Sets the extensions->languageServer mapping
    *
    * @param newExt a Scala map
    */
  def setExtToServ(newExt: collection.Map[String, String]): Unit = extToServLoc = newExt.toMap

  /**
    * Returns the extensions->languageServer mapping
    *
    * @return the Scala map
    */
  def getExtToServLoc: Map[String, String] = extToServLoc

  /**
    * Returns the extensions->languageServer mapping
    *
    * @return The Java map
    */
  def getExtToServLocJava: java.util.Map[String, String] = {
    import scala.collection.JavaConverters._
    extToServLoc.asJava
  }


  /**
    * Called when an editor is opened. Instantiates a LanguageServerWrapper if necessary, and adds the Editor to the Wrapper
    *
    * @param editor the editor
    */
  def editorOpened(editor: Editor): Unit = {
    val file: VirtualFile = FileDocumentManager.getInstance.getFile(editor.getDocument)
    if (file != null) {
      val ext: String = file.getExtension
      extToServLoc.get(ext).foreach(s => {
        var wrapper = extToLanguageWrapper.get(ext).orNull
        if (wrapper == null) {
          LOG.info("Creating wrapper for ext " + ext)
          //TODO what happens when multiple files not in same project ?
          val cp = CoursierImpl.resolveClasspath(s)
          //TODO this line is for dotty
          wrapper = new LanguageServerWrapper(new LanguageServerDefinition(ext), Seq("java", "-cp", cp, dottyM, "-stdio"), Utils.editorToProjectFolderPath(editor))
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

  /**
    * Called when a file is changed. Notifies the server if this file was watched.
    *
    * @param file The file
    */
  def fileChanged(file: VirtualFile): Unit = {
    val uri: String = Utils.VFSToURIString(file)
    uriToLanguageWrapper.get(uri).foreach(l => {
      LOG.info("File saved : " + uri)
      l.getManagerFor(uri).documentSaved()
    })
  }

  /**
    * Called when a file is moved. Notifies the server if this file was watched.
    *
    * @param file The file
    */
  def fileMoved(file: VirtualFile): Unit = {

  }

  /**
    * Called when a file is deleted. Notifies the server if this file was watched.
    *
    * @param file The file
    */
  def fileDeleted(file: VirtualFile): Unit = {

  }

  /**
    * Called when a file is renamed. Notifies the server if this file was watched.
    *
    * @param oldV The old file name
    * @param newV the new file name
    */
  def fileRenamed(oldV: String, newV: String): Unit = {

  }

  /**
    * Called when a file is created. Notifies the server if needed.
    *
    * @param file The file
    */
  def fileCreated(file: VirtualFile): Unit = {

  }

  /**
    * Called when an editor is closed. Notifies the LanguageServerWrapper if needed
    *
    * @param editor the editor.
    */
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

  /**
    * Returns the completion suggestions for a given editor and position
    *
    * @param editor The editor
    * @param pos    The position
    * @return The suggestions
    */
  def completion(editor: Editor, pos: Position): java.lang.Iterable[_ <: LookupElement] = {
    val uri = Utils.editorToURIString(editor)
    import scala.collection.JavaConverters._
    uriToLanguageWrapper.get(uri).map(l => l.getManagerFor(uri).completion(pos)).getOrElse(Iterable()).asJava
  }
}

/**
  * The main class of the plugin
  */
class PluginMain extends ApplicationComponent {

  import com.github.gtache.PluginMain._

  override val getComponentName: String = "PluginMain"

  override def initComponent(): Unit = {
    LSPState.getInstance.getState //Need that to trigger loadState

    EditorFactory.getInstance.addEditorFactoryListener(new EditorListener, Disposer.newDisposable())
    VirtualFileManager.getInstance().addVirtualFileListener(VFSListener)
    LOG.info("PluginMain init finished")
  }
}
