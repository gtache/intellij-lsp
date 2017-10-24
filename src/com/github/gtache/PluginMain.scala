package com.github.gtache

import java.util.concurrent.TimeUnit

import com.github.gtache.client.languageserver._
import com.github.gtache.contributors.LSPNavigationItem
import com.github.gtache.editor.EditorEventManager
import com.github.gtache.editor.listeners.{EditorListener, FileDocumentManagerListenerImpl, VFSListener}
import com.github.gtache.requests.Timeout
import com.github.gtache.settings.LSPState
import com.intellij.AppTopics
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.{Document, Editor, EditorFactory, LogicalPosition}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.{VirtualFile, VirtualFileManager}
import com.intellij.psi.PsiReference
import org.eclipse.lsp4j._

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.TimeoutException

/**
  * The main class of the plugin
  */
object PluginMain {

  private val LOG: Logger = Logger.getInstance(classOf[PluginMain])
  private val extToLanguageWrapper: mutable.Map[(String, String), LanguageServerWrapper] = scala.collection.concurrent.TrieMap()
  private val uriToLanguageWrapper: mutable.Map[String, LanguageServerWrapper] = scala.collection.concurrent.TrieMap()
  private val projectToLanguageWrapper: mutable.Map[Project, LanguageServerWrapper] = scala.collection.concurrent.TrieMap()
  private var extToServerDefinition: Map[String, ServerDefinitionExtensionPoint] = HashMap()
  private var loadedExtensions: Boolean = false

  /**
    * Sets the extensions->languageServer mapping
    *
    * @param newExt a Java Map
    */
  def setExtToServerDefinition(newExt: java.util.Map[String, ServerDefinitionExtensionPoint]): Unit = {
    import scala.collection.JavaConverters._
    setExtToServerDefinition(newExt.asScala)
  }

  /**
    * Sets the extensions->languageServer mapping
    *
    * @param newExt a Scala map
    */
  def setExtToServerDefinition(newExt: collection.Map[String, ServerDefinitionExtensionPoint]): Unit = extToServerDefinition = newExt.toMap

  /**
    * Returns the extensions->languageServer mapping
    *
    * @return the Scala map
    */
  def getExtToServerDefinition: Map[String, ServerDefinitionExtensionPoint] = extToServerDefinition

  /**
    * Returns the extensions->languageServer mapping
    *
    * @return The Java map
    */
  def getExtToServerDefinitionJava: java.util.Map[String, ServerDefinitionExtensionPoint] = {
    import scala.collection.JavaConverters._
    extToServerDefinition.asJava
  }


  /**
    * Called when an editor is opened. Instantiates a LanguageServerWrapper if necessary, and adds the Editor to the Wrapper
    *
    * @param editor the editor
    */
  def editorOpened(editor: Editor): Unit = {
    if (!loadedExtensions) {
      val extensions = ServerDefinitionExtensionPoint.getAllDefinitions.filter(s => !extToServerDefinition.contains(s.ext))
      LOG.info("Added serverDefinitions " + extensions + " from plugins")
      extToServerDefinition = extToServerDefinition ++ extensions.map(s => (s.ext, s))
      loadedExtensions = true
    }
    val file: VirtualFile = FileDocumentManager.getInstance.getFile(editor.getDocument)
    if (file != null) {
      ApplicationManager.getApplication.executeOnPooledThread(new Runnable {
        override def run(): Unit = {
          val ext: String = file.getExtension
          val workingDir: String = Utils.editorToProjectFolderPath(editor)
          LOG.info("Opened a file with extension " + ext)
          extToServerDefinition.get(ext).foreach(s => {
            var wrapper = extToLanguageWrapper.get((ext, workingDir)).orNull
            wrapper match {
              case null =>
                extToLanguageWrapper.put((ext, workingDir), new DummyLanguageServerWrapper)
                wrapper = new LanguageServerWrapperImpl(s,workingDir)
                extToLanguageWrapper.update((ext, workingDir), wrapper)
                projectToLanguageWrapper.put(editor.getProject, wrapper)
              case d: DummyLanguageServerWrapper =>
                while (extToLanguageWrapper((ext, workingDir)).isInstanceOf[DummyLanguageServerWrapper]) {
                  Thread.sleep(500)
                }
                wrapper = extToLanguageWrapper((ext, workingDir))
              case l: LanguageServerWrapperImpl =>
            }
            LOG.info("Adding file " + file.getName)
            wrapper.connect(editor)
            uriToLanguageWrapper.put(Utils.editorToURIString(editor), wrapper)
          })
        }
      })

    } else {
      LOG.warn("File for editor " + editor.getDocument.getText + " is null")
    }
  }

  /**
    * Called when a file is changed. Notifies the server if this file was watched.
    *
    * @param file The file
    */
  def fileChanged(file: VirtualFile): Unit = {
    val uri: String = Utils.VFSToURIString(file)
    if (uri != null) {
      uriToLanguageWrapper.get(uri).foreach(l => {
        LOG.info("File saved : " + uri)
        l.getEditorManagerFor(uri).documentSaved()
      })
    }
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
    val ext = file.getExtension
    val uri = Utils.VFSToURIString(file)
    extToServerDefinition.get(ext) match {
      case Some(s) => // extToLanguageWrapper.get(ext).foreach(f => f.)
      case None =>
    }
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
      extToServerDefinition.get(ext) match {
        case Some(serverDefinition) =>
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
    uriToLanguageWrapper.get(uri).map(l => l.getEditorManagerFor(uri).completion(pos)).getOrElse(Iterable()).asJava
  }

  /**
    * Returns the corresponding workspaceSymbols given a name and a project
    *
    * @param name                   The name to search for
    * @param pattern                The pattern (unused)
    * @param project                The project in which to search
    * @param includeNonProjectItems Whether to search in libraries for example (unused)
    * @param onlyKind               Filter the results to only the kinds in the set (all by default)
    * @return An array of NavigationItem
    */
  def workspaceSymbols(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean = false, onlyKind: Set[SymbolKind] = Set()): Array[NavigationItem] = {
    projectToLanguageWrapper.get(project) match {
      case Some(wrapper) =>
        val params: WorkspaceSymbolParams = new WorkspaceSymbolParams(name)
        val res = wrapper.getRequestManager.symbol(params)
        import scala.collection.JavaConverters._
        try {
          val arr = res.get(Timeout.SYMBOLS_TIMEOUT, TimeUnit.MILLISECONDS).asScala.toArray
          arr.filter(s => if (onlyKind.isEmpty) true else onlyKind.contains(s.getKind)).map(f => {
            LSPNavigationItem(f.getName, f.getContainerName, project, Utils.URIToVFS(f.getLocation.getUri), f.getLocation.getRange.getStart.getLine, f.getLocation.getRange.getStart.getCharacter)
          }).distinct.asInstanceOf[Array[NavigationItem]]
        } catch {
          case e: TimeoutException => LOG.warn(e)
            Array()
        }
      case None => LOG.info("No wrapper for project " + project.getBasePath)
        Array()
    }
  }

  /**
    * Indicates that a document will be saved
    *
    * @param doc The document
    */
  def willSave(doc: Document): Unit = {
    val uri = Utils.VFSToURIString(FileDocumentManager.getInstance().getFile(doc))
    uriToLanguageWrapper.get(uri).foreach(f => f.getEditorManagerFor(uri).willSave())
  }

  /**
    * Indicates that all documents will be saved
    */
  def willSaveAllDocuments(): Unit = {
    uriToLanguageWrapper.foreach(u => u._2.getEditorManagerFor(u._1).willSave())
  }

  /**
    * @param e An Editor
    * @return The LanguageServerWrapper for the given editor
    */
  def getWrapperForEditor(e: Editor): LanguageServerWrapper = {
    val uri = Utils.editorToURIString(e)
    uriToLanguageWrapper.get(uri) match {
      case Some(l) => l
      case None =>
        LOG.warn("No wrapper for editor " + uri)
        null
    }
  }

  /**
    * Asks for references given an editor and a position
    *
    * @param e   The editor
    * @param pos The LogicalPosition
    * @return An Array of PsiReference
    */
  def references(e: Editor, pos: LogicalPosition): Array[PsiReference] = {
    val manager = getManagerForEditor(e)
    if (manager != null) {
      manager.references(pos)
    } else {
      Array()
    }
  }

  /**
    * @param e An Editor
    * @return The EditorEventManager for the given editor
    */
  def getManagerForEditor(e: Editor): EditorEventManager = {
    val uri = Utils.editorToURIString(e)
    getManagerForURI(uri)
  }

  /**
    * @param uri An uri
    * @return The EditorEventManager for the given uri
    */
  def getManagerForURI(uri: String): EditorEventManager = {
    uriToLanguageWrapper.get(uri) match {
      case Some(l) => l.getEditorManagerFor(uri)
      case None =>
        LOG.warn("No wrapper for editor " + uri)
        null
    }
  }

  /**
    * @param uri An uri
    * @return The language server wrapper for the given uri
    */
  def getWrapperForURI(uri: String): LanguageServerWrapper = {
    uriToLanguageWrapper.get(uri).orNull
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
    ApplicationManager.getApplication.getMessageBus.connect().subscribe(AppTopics.FILE_DOCUMENT_SYNC, FileDocumentManagerListenerImpl)
    LOG.info("PluginMain init finished")
  }
}
