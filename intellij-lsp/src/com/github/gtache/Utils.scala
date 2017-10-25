package com.github.gtache

import java.io.File
import java.net.{MalformedURLException, URI, URL}

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.{LocalFileSystem, VirtualFile}
import org.eclipse.lsp4j.{Position, TextDocumentIdentifier}

/**
  * Object containing some useful methods for the plugin
  */
object Utils {

  private val LOG: Logger = Logger.getInstance(Utils.getClass)

  /**
    * Returns a file type given an editor
    *
    * @param editor The editor
    * @return The FileType
    */
  def fileTypeFromEditor(editor: Editor): FileType = {
    FileDocumentManager.getInstance().getFile(editor.getDocument).getFileType
  }

  /**
    * Transforms an editor (Document) identifier to an LSP identifier
    *
    * @param editor The editor
    * @return The TextDocumentIdentifier
    */
  def editorToLSPIdentifier(editor: Editor): TextDocumentIdentifier = {
    new TextDocumentIdentifier(editorToURIString(editor))
  }

  /**
    * Returns the URI string corresponding to an Editor (Document)
    *
    * @param editor The Editor
    * @return The URI
    */
  def editorToURIString(editor: Editor): String = {
    val uri = new URL(FileDocumentManager.getInstance().getFile(editor.getDocument).getUrl).toURI.toString
    LOG.info("From " + editor + " to " + uri)
    uri
  }

  /**
    * Returns the URI string corresponding to a VirtualFileSystem file
    *
    * @param file The file
    * @return the URI
    */
  def VFSToURIString(file: VirtualFile): String = {
    try {
      val uri = new URL(file.getUrl).toURI.toString
      LOG.info("From " + file.getCanonicalPath + " to " + uri)
      uri
    } catch {
      case e: MalformedURLException =>
        LOG.warn(e)
        null
    }
  }

  /**
    * Transforms an URI string into a VFS file
    *
    * @param uri The uri
    * @return The virtual file
    */
  def URIToVFS(uri: String): VirtualFile = {
    val res = LocalFileSystem.getInstance().findFileByPath(new File(new URI(uri).getPath).getAbsolutePath)
    LOG.info("From " + uri + " to " + res)
    res
  }

  /**
    * Returns the project base dir uri given an editor
    *
    * @param editor The editor
    * @return The project whose the editor belongs
    */
  def editorToProjectFolderUri(editor: Editor): String = {
    new File(editor.getProject.getBaseDir.getPath).toURI.toString
  }

  /**
    * Transforms a path into an URI string
    *
    * @param path The path
    * @return The uri
    */
  def pathToUri(path: String): String = {
    new File(path).toURI.toString
  }

  /**
    * Calculates a Position given an editor and an offset
    *
    * @param editor The editor
    * @param offset The offset
    * @return an LSP position
    */
  def offsetToLSPPos(editor: Editor, offset: Int): Position = {
    logicalToLSPPos(editor.offsetToLogicalPosition(offset))
  }

  /**
    * Transforms a LogicalPosition (IntelliJ) to an LSP Position
    *
    * @param position the LogicalPosition
    * @return the Position
    */
  def logicalToLSPPos(position: LogicalPosition): Position = {
    new Position(position.line, position.column)
  }

  /**
    * Transforms an LSP position to an editor offset
    *
    * @param editor The editor
    * @param pos    The LSPPos
    * @return The offset
    */
  def LSPPosToOffset(editor: Editor, pos: Position): Int = {
    editor.logicalPositionToOffset(LSPToLogicalPos(pos))
  }

  /**
    * Transforms an LSP position to a LogicalPosition
    *
    * @param position The LSPPos
    * @return The LogicalPos
    */
  def LSPToLogicalPos(position: Position): LogicalPosition = {
    new LogicalPosition(position.getLine, position.getCharacter)
  }

  /**
    * Transforms an array into a string (using mkString, useful for Java)
    *
    * @param arr The array
    * @param sep A separator
    * @return The result of mkString
    */
  def arrayToString(arr: Array[Any], sep: String = ""): String = {
    arr.mkString(sep)
  }

  /**
    * Transforms a (java) Map<String, ServerDefinitionExtensionPointArtifact> to a Map<String, String[]>
    *
    * @param map A java map
    * @return the transformed java map
    */
  def serverDefinitionArtifactMapToArrayMap(map: java.util.Map[String, ServerDefinitionExtensionPointArtifact]): java.util.Map[String, Array[String]] = {
    import scala.collection.JavaConverters._
    map.asScala.map(e => (e._1, serverDefinitionArtifactToArray(e._2))).asJava
  }


  /**
    * Transforms a ServerDefinitionExtensionPointArtifact into an array of String
    *
    * @param serverDefinitionExtensionPoint The ServerDefinition
    * @return The Array of string
    */
  def serverDefinitionArtifactToArray(serverDefinitionExtensionPoint: ServerDefinitionExtensionPointArtifact): Array[String] = {
    Array(serverDefinitionExtensionPoint.ext, serverDefinitionExtensionPoint.packge, serverDefinitionExtensionPoint.mainClass) ++ serverDefinitionExtensionPoint.args
  }

  /**
    * Transforms a (java) Map<String, String[]> to a Map<String, ServerDefinitionExtensionPointArtifact>
    *
    * @param map A java map
    * @return the transformed java map
    */
  def arrayMapToServerDefinitionArtifactMap(map: java.util.Map[String, Array[String]]): java.util.Map[String, ServerDefinitionExtensionPointArtifact] = {
    import scala.collection.JavaConverters._
    map.asScala.map(e => (e._1, arrayToServerDefinitionArtifact(e._2))).asJava
  }

  /**
    * Transforms an array of string into a ServerDefinitionExtensionPointArtifact
    *
    * @param arr The array of string
    * @return The corresponding ServerDefinitionExtensionPoint
    */
  def arrayToServerDefinitionArtifact(arr: Array[String]): ServerDefinitionExtensionPointArtifact = {
    if (arr.length < 3) {
      LOG.warn("Not enough elements to translate into a ServerDefinition : " + arr)
      null
    } else {
      ServerDefinitionExtensionPointArtifact(arr.head, arr.tail.head, arr.tail.tail.head, if (arr.length > 3) arr.tail.tail.tail else Array())
    }
  }


}
