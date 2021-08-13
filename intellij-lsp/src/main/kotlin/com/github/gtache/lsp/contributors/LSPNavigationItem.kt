package com.github.gtache.lsp.contributors

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * An LSP NavigationItem
 *
 * @param name      The name of the item
 * @param container The container name (a file, ...)
 * @param project   The project the item belongs to
 * @param file      The file it belongs to
 * @param line      Its line
 * @param col       Its column
 */
data class LSPNavigationItem(
    private val name: String,
    private val container: String,
    private val cproject: Project,
    private val cfile: VirtualFile,
    private val cline: Int,
    private val col: Int,
    private val icon: Icon? = null
) : OpenFileDescriptor(cproject, cfile, cline, col), NavigationItem {

    companion object {
        private val logger: Logger = Logger.getInstance(LSPNavigationItem::class.java)
    }

    override fun getName(): String = name

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {

        override fun getPresentableText(): String = name

        override fun getLocationString(): String = container + name

        override fun getIcon(unused: Boolean): Icon? = if (unused) null else icon
    }
}