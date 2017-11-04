package com.github.gtache

import java.util.concurrent.{TimeUnit, TimeoutException}

import com.github.gtache.client.languageserver._
import com.github.gtache.contributors.LSPNavigationItem
import com.github.gtache.editor.EditorEventManager
import com.github.gtache.editor.listeners.{EditorListener, FileDocumentManagerListenerImpl, VFSListener}
import com.github.gtache.requests.Timeout
import com.github.gtache.settings.LSPState
import com.github.gtache.utils.Utils
import com.intellij.AppTopics
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.{Editor, EditorFactory, LogicalPosition}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.{VirtualFile, VirtualFileManager}
import com.intellij.psi.PsiReference
import org.eclipse.lsp4j._

import scala.collection.immutable.HashMap
import scala.collection.mutable

/**
  * The main class of the plugin
  */
object PluginMain {

  private val LOG: Logger = Logger.getInstance(classOf[PluginMain])
  private val extToLanguageWrapper: mutable.Map[(String, String), LanguageServerWrapper] = mutable.HashMap()
  private val projectToLanguageWrappers: mutable.Map[String, mutable.Set[LanguageServerWrapper]] = mutable.HashMap()
  private var extToServerDefinition: Map[String, LanguageServerDefinition] = HashMap()
  private var loadedExtensions: Boolean = false


  /**
    * @param ext An extension
    * @return True if there is a LanguageServer supporting this extension, false otherwise
    */
  def isExtensionSupported(ext: String): Boolean = {
    extToServerDefinition.contains(ext)
  }

  /**
    * Sets the extensions->languageServer mapping
    *
    * @param newExt a Java Map
    */
  def setExtToServerDefinition(newExt: java.util.Map[String, _ <: LanguageServerDefinition]): Unit = {
    import scala.collection.JavaConverters._
    setExtToServerDefinition(newExt.asScala)
  }

  /**
    * Sets the extensions->languageServer mapping
    *
    * @param newExt a Scala map
    */
  def setExtToServerDefinition(newExt: collection.Map[String, _ <: LanguageServerDefinition]): Unit = {
    val nullDef = newExt.filter(d => d._2 == null)
    nullDef.foreach(ext => LOG.error("Definition for " + ext + " is null"))
    extToServerDefinition = newExt.toMap.filter(d => d._2 != null)
  }

  /**
    * Returns the extensions->languageServer mapping
    *
    * @return the Scala map
    */
  def getExtToServerDefinition: Map[String, LanguageServerDefinition] = extToServerDefinition

  /**
    * Returns the extensions->languageServer mapping
    *
    * @return The Java map
    */
  def getExtToServerDefinitionJava: java.util.Map[String, LanguageServerDefinition] = {
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
      val extensions = LanguageServerDefinition.getAllDefinitions.filter(s => !extToServerDefinition.contains(s.ext))
      LOG.info("Added serverDefinitions " + extensions + " from plugins")
      extToServerDefinition = extToServerDefinition ++ extensions.map(s => (s.ext, s))
      loadedExtensions = true
    }
    val file: VirtualFile = FileDocumentManager.getInstance.getFile(editor.getDocument)
    if (file != null) {
      ApplicationManager.getApplication.executeOnPooledThread(new Runnable {
        override def run(): Unit = {
          val ext: String = file.getExtension
          val rootPath: String = Utils.editorToProjectFolderPath(editor)
          val rootUri: String = Utils.pathToUri(rootPath)
          LOG.info("Opened " + file.getName)
          extToServerDefinition.get(ext).foreach(s => {
            extToLanguageWrapper.synchronized {
              var wrapper = extToLanguageWrapper.get((ext, rootUri)).orNull
              wrapper match {
                case null =>
                  LOG.info("Instantiating wrapper for " + ext + " : " + rootUri)
                  wrapper = new LanguageServerWrapperImpl(s, rootPath)
                  extToLanguageWrapper.put((ext, rootUri), wrapper)
                  projectToLanguageWrappers.get(rootUri) match {
                    case Some(set) =>
                      set.add(wrapper)
                    case None =>
                      projectToLanguageWrappers.put(rootUri, mutable.Set(wrapper))
                  }
                case l: LanguageServerWrapperImpl =>
                  LOG.info("Wrapper already existing for " + ext + " : " + rootUri)
              }
              LOG.info("Adding file " + file.getName)
              wrapper.connect(editor)
            }
          })
        }
      })

    } else {
      LOG.warn("File for editor " + editor.getDocument.getText + " is null")
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
      extToServerDefinition.get(ext) match {
        case Some(_) =>
          val uri = Utils.editorToURIString(editor)
          ApplicationManager.getApplication.executeOnPooledThread(new Runnable {
            override def run(): Unit = LanguageServerWrapperImpl.forUri(uri).foreach(l => {
              LOG.info("Disconnecting " + uri)
              l.disconnect(uri)
            })
          })
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
    import scala.collection.JavaConverters._
    EditorEventManager.forEditor(editor).map(e => e.completion(pos)).getOrElse(Iterable()).asJava
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
    projectToLanguageWrappers.get(Utils.pathToUri(project.getBasePath)) match {
      case Some(set) =>
        val params: WorkspaceSymbolParams = new WorkspaceSymbolParams(name)
        val res = set.map(f => f.getRequestManager.symbol(params)).toSet
        if (!res.contains(null)) {
          try {
            import scala.collection.JavaConverters._
            val arr = res.flatMap(r => r.get(Timeout.SYMBOLS_TIMEOUT, TimeUnit.MILLISECONDS).asInstanceOf[java.util.List[SymbolInformation]].asScala.toSet)
            arr.filter(s => if (onlyKind.isEmpty) true else onlyKind.contains(s.getKind)).map(f => {
              val start = f.getLocation.getRange.getStart
              val uri = Utils.URIToVFS(f.getLocation.getUri)
              LSPNavigationItem(f.getName, f.getContainerName, project, uri, start.getLine, start.getCharacter)
            }).toArray.asInstanceOf[Array[NavigationItem]]
          } catch {
            case e: TimeoutException => LOG.warn(e)
              Array()
          }
        } else Array.empty
      case None => LOG.info("No wrapper for project " + project.getBasePath)
        Array.empty
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
    EditorEventManager.forEditor(e).map(e => e.references(pos)).getOrElse(Array())
  }

  /**
    * Notifies that a LanguageServer was stopped
    *
    * @param wrapper The wrapper of the languageServer
    */
  def languageServerStopped(wrapper: LanguageServerWrapper): Unit = {
    projectToLanguageWrappers.find(p => p._2.contains(wrapper)).foreach(found => projectToLanguageWrappers.remove(found._1))
    extToLanguageWrapper.find(p => p._2 == wrapper).foreach(found => extToLanguageWrapper.remove(found._1))
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
