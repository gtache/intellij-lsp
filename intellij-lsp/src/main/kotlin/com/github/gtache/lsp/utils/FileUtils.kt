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
    val os = if (System.getProperty("os.name").lowercase().contains("win")) OS.WINDOWS else OS.UNIX
    const val COLON_ENCODED: String = "%3A"
    const val SPACE_ENCODED: String = "%20"
    const val URI_FILE_BEGIN: String = "file:"
    const val URI_VALID_FILE_BEGIN: String = "$URI_FILE_BEGIN///"
    const val URI_PATH_SEP: Char = '/'

    const val LSP_ROOT_DIR: String = "lsp/"
    const val LSP_LOG_DIR: String = LSP_ROOT_DIR + "log/"
    const val LSP_CONFIG_DIR: String = LSP_ROOT_DIR + "conf/"

    private val logger: Logger = Logger.getInstance(FileUtils::class.java)

    fun extFromPsiFile(psiFile: PsiFile): String? {
        return psiFile.virtualFile.extension
    }

    fun editorFromPsiFile(psiFile: PsiFile): Editor? {
        return editorFromVirtualFile(psiFile.virtualFile, psiFile.project)
    }

    fun editorFromUri(uri: String, project: Project): Editor? {
        return URIToVFS(uri)?.let { editorFromVirtualFile(it, project) }
    }

    fun editorFromVirtualFile(file: VirtualFile, project: Project): Editor? {
        return FileEditorManager.getInstance(project).getAllEditors(file).filterIsInstance<TextEditor>().map { t -> t.editor }.firstOrNull()
    }

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
     * Returns a file type given an editor
     *
     * @param editor The editor
     * @return The FileType
     */
    fun fileTypeFromEditor(editor: Editor): FileType? {
        return FileDocumentManager.getInstance().getFile(editor.document)?.fileType
    }

    fun extFromEditor(editor: Editor): String? {
        return FileDocumentManager.getInstance().getFile(editor.document)?.extension
    }

    /**
     * Transforms an editor (Document) identifier to an LSP identifier
     *
     * @param editor The editor
     * @return The TextDocumentIdentifier
     */
    fun editorToLSPIdentifier(editor: Editor): TextDocumentIdentifier {
        return TextDocumentIdentifier(editorToURIString(editor))
    }

    /**
     * Returns the URI string corresponding to an Editor (Document)
     *
     * @param editor The Editor
     * @return The URI
     */
    fun editorToURIString(editor: Editor): String? {
        val file = FileDocumentManager.getInstance().getFile(editor.document)
        return file?.let {
            VFSToURI(it)?.let { uri ->
                sanitizeURI(uri)
            }
        }
    }

    /**
     * Returns the URI string corresponding to a VirtualFileSystem file
     *
     * @param file The file
     * @return the URI
     */
    fun VFSToURI(file: VirtualFile): String? {
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
     * Fixes common problems in uri, mainly related to Windows
     *
     * @param uri The uri to sanitize
     * @return The sanitized uri
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
     * Transforms an URI string into a VFS file
     *
     * @param uri The uri
     * @return The virtual file
     */
    fun URIToVFS(uri: String): VirtualFile? {
        return LocalFileSystem.getInstance().findFileByIoFile(File(URI(sanitizeURI(uri))))
    }

    /**
     * Returns the project base dir uri given an editor
     *
     * @param editor The editor
     * @return The project whose the editor belongs
     */
    fun editorToProjectFolderUri(editor: Editor): String? {
        return editorToProjectFolderPath(editor)?.let {
            pathToUri(it)
        }
    }

    fun editorToProjectFolderPath(editor: Editor): String? {
        val project = editor.project
        return if (project != null && !project.isDefault) {
            project.guessProjectDir()?.let {
                VFSToPath(it)
            }
        } else null
    }

    fun VFSToPath(file: VirtualFile): String {
        return File(file.path).absolutePath
    }

    /**
     * Transforms a path into an URI string
     *
     * @param path The path
     * @return The uri
     */
    fun pathToUri(path: String): String? {
        return sanitizeURI(File(path.replace(" ", SPACE_ENCODED)).toURI().toString())
    }

    fun uriToPath(uri: String): String {
        return File(URI(uri)).absolutePath
    }

    fun projectToUri(project: Project): String? {
        return project.basePath?.let {
            pathToUri(File(it).absolutePath)
        }
    }

    fun documentToUri(document: Document): String? {
        val file = FileDocumentManager.getInstance().getFile(document)
        return if (file != null) {
            VFSToURI(file)?.let {
                sanitizeURI(it)
            }
        } else null
    }

    fun getAllOpenedEditors(project: Project): List<Editor> {
        return ApplicationUtils.computableReadAction { FileEditorManager.getInstance(project).allEditors.filterIsInstance<TextEditor>().map { it.editor } }
    }

    /**
     * Object representing the OS type (Windows or Unix)
     */
    enum class OS {
        WINDOWS, UNIX
    }

}