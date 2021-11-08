package com.github.gtache.lsp.utils

import com.github.gtache.lsp.utils.ApplicationUtils.computableWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.TextDocumentIdentifier
import java.io.File
import java.net.URI
import java.net.URL

/**
 * Various file / uri related methods
 */
object FileUtils {
    private val os = if (System.getProperty("os.name").lowercase().contains("win")) OS.WINDOWS else OS.UNIX

    /**
     * Object representing the OS type (Windows or Unix)
     */
    private enum class OS {
        WINDOWS, UNIX
    }

    const val SPACE_ENCODED: String = "%20"
    private const val COLON_ENCODED: String = "%3A"
    private const val URI_FILE_BEGIN: String = "file:"
    private const val URI_VALID_FILE_BEGIN: String = "$URI_FILE_BEGIN///"
    private const val URI_PATH_SEP: Char = '/'

    private const val LSP_ROOT_DIR: String = "lsp/"
    const val LSP_LOG_DIR: String = LSP_ROOT_DIR + "log/"
    const val LSP_CONFIG_DIR: String = LSP_ROOT_DIR + "conf/"

    private val logger: Logger = Logger.getInstance(FileUtils::class.java)

    /**
     * Returns the extension of a [psiFile]
     */
    fun extFromPsiFile(psiFile: PsiFile): String? {
        return psiFile.virtualFile.extension
    }

    /**
     * Returns the editor of a [psiFile]
     */
    fun editorFromPsiFile(psiFile: PsiFile): Editor? {
        return editorFromVirtualFile(psiFile.virtualFile, psiFile.project)
    }

    /**
     * Returns the editor given an [uri] and a [project]
     */
    fun editorFromUri(uri: String, project: Project): Editor? {
        return uriToVFS(uri)?.let { editorFromVirtualFile(it, project) }
    }

    /**
     * Returns the editor given a virtual [file] and a [project]
     */
    fun editorFromVirtualFile(file: VirtualFile, project: Project): Editor? {
        return FileEditorManager.getInstance(project).getAllEditors(file).filterIsInstance<TextEditor>().map { t -> t.editor }.firstOrNull()
    }

    /**
     * Opens an editor given an [uri] and a [project] and returns a pair of (virtualFile, editor)
     */
    fun openClosedEditor(uri: String, project: Project): Pair<VirtualFile, Editor>? {
        val file = LocalFileSystem.getInstance().findFileByIoFile(File(URI(sanitizeURI(uri))))
        return if (file != null) {
            val descriptor = OpenFileDescriptor(project, file)
            val fileEditorManager = FileEditorManager.getInstance(project)
            val editor = computableWriteAction(Computable {
                fileEditorManager.openTextEditor(descriptor, false)
            })
            if (editor != null) {
                Pair(file, editor)
            } else null
        } else null
    }

    /**
     * Returns a file type given an [editor]
     */
    fun fileTypeFromEditor(editor: Editor): FileType? {
        return FileDocumentManager.getInstance().getFile(editor.document)?.fileType
    }

    /**
     * Returns the extension of a file opened in an [editor]
     */
    fun extFromEditor(editor: Editor): String? {
        return FileDocumentManager.getInstance().getFile(editor.document)?.extension
    }

    /**
     * Transforms an [editor] identifier to an LSP identifier
     */
    fun editorToLSPIdentifier(editor: Editor): TextDocumentIdentifier {
        return TextDocumentIdentifier(editorToURIString(editor))
    }

    /**
     * Returns the URI string corresponding to an [editor]
     */
    fun editorToURIString(editor: Editor): String? {
        val file = FileDocumentManager.getInstance().getFile(editor.document)
        return file?.let {
            vfsToURI(it)?.let { uri ->
                sanitizeURI(uri)
            }
        }
    }

    /**
     * Returns the URI string corresponding to a virtual [file]
     */
    fun vfsToURI(file: VirtualFile): String? {
        return if (!file.url.startsWith("mock") && !file.url.startsWith("dbSrc")) {
            try {
                sanitizeURI(URL(file.url.replace(" ", SPACE_ENCODED)).toURI().toString())
            } catch (e: Exception) {
                logger.warn(e)
                logger.warn("Caused by " + file.url)
                null
            }
        } else null
    }

    /**
     * Returns a sanitized uri by fixing common problems in a [uri], mainly related to Windows
     */
    fun sanitizeURI(uri: String): String {
        val reconstructed = StringBuilder()
        var uriCp = uri.replace(" ", SPACE_ENCODED) //Don't trust servers
        if (!uri.startsWith(URI_FILE_BEGIN)) {
            logger.warn("Malformed uri : $uri")
            return uri //Probably not an uri
        } else {
            uriCp = uriCp.drop(URI_FILE_BEGIN.length).dropWhile { c -> c == URI_PATH_SEP }
            reconstructed.append(URI_VALID_FILE_BEGIN)
            if (os == OS.UNIX) {
                return reconstructed.append(uriCp).toString()
            } else {
                reconstructed.append(uriCp.takeWhile { c -> c != URI_PATH_SEP })
                val driveLetter = reconstructed[URI_VALID_FILE_BEGIN.length]
                if (driveLetter.isLowerCase()) {
                    reconstructed.setCharAt(URI_VALID_FILE_BEGIN.length, driveLetter.uppercaseChar())
                }
                if (reconstructed.endsWith(COLON_ENCODED)) {
                    reconstructed.delete(reconstructed.length - 3, reconstructed.length)
                }
                if (!reconstructed.endsWith(":")) {
                    reconstructed.append(":")
                }
                return reconstructed.append(uriCp.dropWhile { c -> c != URI_PATH_SEP }).toString()
            }

        }
    }

    /**
     * Transforms an [uri] string into a VFS file
     */
    fun uriToVFS(uri: String): VirtualFile? {
        return LocalFileSystem.getInstance().findFileByIoFile(File(URI(sanitizeURI(uri))))
    }

    /**
     * Returns the project base dir uri given an [editor]
     */
    fun editorToProjectFolderUri(editor: Editor): String? {
        return editorToProjectFolderPath(editor)?.let {
            pathToUri(it)
        }
    }

    /**
     * Returns the project base dir path given an [editor]
     */
    fun editorToProjectFolderPath(editor: Editor): String? {
        val project = editor.project
        return if (project != null && !project.isDefault) {
            project.guessProjectDir()?.let {
                vfsToPath(it)
            }
        } else null
    }

    /**
     * Transform a virtual [file] to a path
     */
    fun vfsToPath(file: VirtualFile): String {
        return File(file.path).absolutePath
    }

    /**
     * Transforms a [path] into an URI string
     */
    fun pathToUri(path: String): String {
        return sanitizeURI(File(path.replace(" ", SPACE_ENCODED)).toURI().toString())
    }

    /**
     * Transforms an [uri] string to a path
     */
    fun uriToPath(uri: String): String {
        return File(URI(uri)).absolutePath
    }

    /**
     * Gets the uri of a [project]
     */
    fun projectToUri(project: Project): String? {
        return project.basePath?.let {
            pathToUri(File(it).absolutePath)
        }
    }

    /**
     * Gets the uri of a [document]
     */
    fun documentToUri(document: Document): String? {
        val file = FileDocumentManager.getInstance().getFile(document)
        return if (file != null) {
            vfsToURI(file)?.let {
                sanitizeURI(it)
            }
        } else null
    }

    /**
     * Retrieves all the currently opened editors for a [project]
     */
    fun getAllOpenedEditors(project: Project): List<Editor> {
        return ApplicationUtils.computableReadAction { FileEditorManager.getInstance(project).allEditors.filterIsInstance<TextEditor>().map { it.editor } }
    }


}