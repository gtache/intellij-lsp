package com.github.gtache.lsp.contributors.psi

import com.github.gtache.lsp.utils.ApplicationUtils
import com.github.gtache.lsp.utils.FileUtils
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import com.intellij.util.concurrency.AtomicFieldUpdater
import com.intellij.util.keyFMap.KeyFMap
import javax.swing.Icon

/**
 * A simple PsiElement for LSP
 *
 * @param name    The name (text) of the element
 * @param project The project it belongs to
 * @param start   The offset in the editor where the element starts
 * @param end     The offset where it ends
 */
data class LSPPsiElement(private var name: String, private val project: Project, val start: Int, val end: Int, val file: PsiFile, val editor: Editor) :
    PsiNameIdentifierOwner,
    NavigatablePsiElement {

    companion object {
        private val COPYABLE_USER_MAP_KEY: Key<KeyFMap> = Key.create("COPYABLE_USER_MAP_KEY")
    }

    private val updater = AtomicFieldUpdater.forFieldOfType(LSPPsiElement::class.java, KeyFMap::class.java)
    private val manager = PsiManager.getInstance(project)
    private val reference = LSPPsiReference(this)
    //private val iconProvider : LSPIconProvider = GUIUtils.getIconProviderFor(PluginMain.getExtToServerDefinition.get(file.getVirtualFile.getExtension).orNull)

    /**
     * Concurrent writes to this field are via CASes only, using the {@link #updater}
     */
    @Volatile
    private var myUserMap: KeyFMap = KeyFMap.EMPTY_MAP

    /**
     * Returns the language of the PSI element.
     *
     * Returns the language instance.
     */
    override fun getLanguage(): Language = PlainTextLanguage.INSTANCE

    /**
     * Returns the PSI manager for the project to which the PSI element belongs.
     *
     * Returns the PSI manager instance.
     */
    override fun getManager(): PsiManager = manager

    /**
     * Returns the array of children for the PSI element.
     * Important: In some implementations children are only composite elements, i.e. not a leaf elements
     *
     * Returns the array of child elements.
     */
    override fun getChildren(): Array<PsiElement>? = null

    /**
     * Returns the parent of the PSI element.
     *
     * Returns the parent of the element, or null if the element has no parent.
     */
    override fun getParent(): PsiElement = containingFile

    /**
     * Returns the first child of the PSI element.
     *
     * Returns the first child, or null if the element has no children.
     */
    override fun getFirstChild(): PsiElement? = null

    /**
     * Returns the last child of the PSI element.
     *
     * Returns the last child, or null if the element has no children.
     */
    override fun getLastChild(): PsiElement? = null

    /**
     * Returns the next sibling of the PSI element.
     *
     * Returns the next sibling, or null if the node is the last in the list of siblings.
     */
    override fun getNextSibling(): PsiElement? = null

    /**
     * Returns the previous sibling of the PSI element.
     *
     * Returns the previous sibling, or null if the node is the first in the list of siblings.
     */
    override fun getPrevSibling(): PsiElement? = null

    /**
     * Returns the text range in the document occupied by the PSI element.
     *
     * Returns the text range.
     */
    override fun getTextRange(): TextRange = TextRange(start, end)

    /**
     * Returns the text offset of the PSI element relative to its parent.
     *
     * Returns the relative offset.
     */
    override fun getStartOffsetInParent(): Int = start

    /**
     * Returns the length of text of the PSI element.
     *
     * Returns the text length.
     */
    override fun getTextLength(): Int = end - start

    /**
     * Finds a leaf PSI element at the specified offset from the start of the text range of this node.
     *
     * @param offset the relative offset for which the PSI element is requested.
     * Returns the element at the offset, or null if none is found.
     */
    override fun findElementAt(offset: Int): PsiElement? = null

    /**
     * Finds a reference at the specified offset from the start of the text range of this node.
     *
     * @param offset the relative offset for which the reference is requested.
     * Returns the reference at the offset, or null if none is found.
     */
    override fun findReferenceAt(offset: Int): PsiReference? = null

    /**
     * Returns the text of the PSI element as a character array.
     *
     * Returns the element text as a character array.
     */
    override fun textToCharArray(): CharArray = name.toCharArray()

    /**
     * Returns the PSI element which should be used as a navigation target
     * when navigation to this PSI element is requested. The method can either
     * return {@code this} or substitute a different element if this element
     * does not have an associated file and offset. (For example, if the source code
     * of a library is attached to a project, the navigation element for a compiled
     * library class is its source class.)
     *
     * Returns the navigation target element.
     */
    override fun getNavigationElement(): PsiElement = this

    /**
     * Returns the PSI element which corresponds to this element and belongs to
     * either the project source path or class path. The method can either return
     * {@code this} or substitute a different element if this element does
     * not belong to the source path or class path. (For example, the original
     * element for a library source file is the corresponding compiled class file.)
     *
     * Returns the original element.
     */
    override fun getOriginalElement(): PsiElement? = null

    /**
     * Checks if the text of this PSI element is equal to the specified character sequence.
     *
     * @param text the character sequence to compare ,.
     * Returns true if the text is equal, false otherwise.
     */
    override fun textMatches(text: CharSequence): Boolean = this.text == text

    //Q: get rid of these methods?

    /**
     * Checks if the text of this PSI element is equal to the text of the specified PSI element.
     *
     * @param element the element to compare the text ,.
     * Returns true if the text is equal, false otherwise.
     */
    override fun textMatches(element: PsiElement): Boolean = text == element.text

    /**
     * Checks if the text of this element contains the specified character.
     *
     * @param c the character to search for.
     * Returns true if the character is found, false otherwise.
     */
    override fun textContains(c: Char): Boolean = text.contains(c)

    /**
     * Returns the text of the PSI element.
     *
     * Returns the element text.
     */
    override fun getText(): String = name

    /**
     * Passes the element to the specified visitor.
     *
     * @param visitor the visitor to pass the element to.
     */
    override fun accept(visitor: PsiElementVisitor): Unit {
        visitor.visitElement(this)
    }

    /**
     * Passes the children of the element to the specified visitor.
     *
     * @param visitor the visitor to pass the children to.
     */
    override fun acceptChildren(visitor: PsiElementVisitor): Unit {}

    /**
     * Creates a copy of the file containing the PSI element and returns the corresponding
     * element in the created copy. Resolve operations performed on elements in the copy
     * of the file will resolve to elements in the copy, not in the original file.
     *
     * Returns the element in the file copy corresponding to this element.
     */
    override fun copy(): PsiElement? = null

    /**
     * Adds a child to this PSI element.
     *
     * @param element the child element to add.
     * Returns the element which was actually added (either { @code element} or its copy).
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     */
    override fun add(element: PsiElement): PsiElement = throw IncorrectOperationException()

    /**
     * Adds a child to this PSI element, before the specified anchor element.
     *
     * @param element the child element to add.
     * @param anchor  the anchor before which the child element is inserted (must be a child of this PSI element)
     * Returns the element which was actually added (either { @code element} or its copy).
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     */
    override fun addBefore(element: PsiElement, anchor: PsiElement?): PsiElement = throw IncorrectOperationException()

    /**
     * Adds a child to this PSI element, after the specified anchor element.
     *
     * @param element the child element to add.
     * @param anchor  the anchor after which the child element is inserted (must be a child of this PSI element)
     * Returns the element which was actually added (either { @code element} or its copy).
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     */
    override fun addAfter(element: PsiElement, anchor: PsiElement?): PsiElement = throw IncorrectOperationException()

    /**
     * Checks if it is possible to add the specified element as a child to this element,
     * and throws an exception if the add is not possible. Does not actually modify anything.
     *
     * @param element the child element to check the add possibility.
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     * @deprecated not all PSI implementations implement this method correctly.
     */
    override fun checkAdd(element: PsiElement): Unit = throw IncorrectOperationException()

    /**
     * Adds a range of elements as children to this PSI element.
     *
     * @param first the first child element to add.
     * @param last  the last child element to add (must have the same parent as { @code first})
     * Returns the first child element which was actually added (either { @code first} or its copy).
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     */
    override fun addRange(first: PsiElement, last: PsiElement): PsiElement = throw IncorrectOperationException()

    /**
     * Adds a range of elements as children to this PSI element, before the specified anchor element.
     *
     * @param first  the first child element to add.
     * @param last   the last child element to add (must have the same parent as { @code first})
     * @param anchor the anchor before which the child element is inserted (must be a child of this PSI element)
     * Returns the first child element which was actually added (either { @code first} or its copy).
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     */
    override fun addRangeBefore(first: PsiElement, last: PsiElement, anchor: PsiElement): PsiElement = throw IncorrectOperationException()

    /**
     * Adds a range of elements as children to this PSI element, after the specified anchor element.
     *
     * @param first  the first child element to add.
     * @param last   the last child element to add (must have the same parent as { @code first})
     * @param anchor the anchor after which the child element is inserted (must be a child of this PSI element)
     * Returns the first child element which was actually added (either { @code first} or its copy).
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     */
    override fun addRangeAfter(first: PsiElement, last: PsiElement, anchor: PsiElement): PsiElement = throw IncorrectOperationException()

    /**
     * Deletes this PSI element from the tree.
     *
     * @throws IncorrectOperationException if the modification is not supported
     *                                     or not possible for some reason (for example, the file containing the element is read-only).
     */
    override fun delete(): Unit = throw IncorrectOperationException()

    /**
     * Checks if it is possible to delete the specified element from the tree,
     * and throws an exception if the add is not possible. Does not actually modify anything.
     *
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     * @deprecated not all PSI implementations implement this method correctly.
     */
    override fun checkDelete(): Unit = throw IncorrectOperationException()

    /**
     * Deletes a range of children of this PSI element from the tree.
     *
     * @param first the first child to delete (must be a child of this PSI element)
     * @param last  the last child to delete (must be a child of this PSI element)
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     */
    override fun deleteChildRange(first: PsiElement, last: PsiElement): Unit = throw IncorrectOperationException()

    /**
     * Replaces this PSI element (along , all its children) , another element
     * (along , the children).
     *
     * @param newElement the element to replace this element ,.
     * Returns the element which was actually inserted in the tree (either { @code newElement} or its copy)
     * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
     */
    override fun replace(newElement: PsiElement): PsiElement = throw IncorrectOperationException()

    /**
     * Checks if this PSI element is valid. Valid elements and their hierarchy members
     * can be accessed for reading and writing. Valid elements can still correspond to
     * underlying documents whose text is different, when those documents have been changed
     * and not yet committed ({@link PsiDocumentManager#commitDocument(com.intellij.openapi.editor.Document)}).
     * (In this an attempt to change PSI will result in an exception).
     *
     * Any access to invalid elements results in {@link PsiInvalidElementAccessException}.
     *
     * Once invalid, elements can't become valid again.
     *
     * Elements become invalid in following cases:
     * <ul>
     * <li>They have been deleted via PSI operation ({@link #delete()})</li>
     * <li>They have been deleted as a result of an incremental reparse (document commit)</li>
     * <li>Their containing file has been changed externally, or renamed so that its PSI had to be rebuilt from scratch</li>
     * </ul>
     *
     * Returns true if the element is valid, false otherwise.
     * @see com.intellij.psi.util.PsiUtilCore#ensureValid(PsiElement)
     */
    override fun isValid(): Boolean = true

    /**
     * Checks if the contents of the element can be modified (if it belongs to a
     * non-read-only source file.)
     *
     * Returns true if the element can be modified, false otherwise.
     */
    override fun isWritable(): Boolean = true

    /**
     * Returns the reference from this PSI element to another PSI element (or elements), if one exists.
     * If the element has multiple associated references (see {@link #getReferences()}
     * for an example), returns the first associated reference.
     *
     * Returns the reference instance, or null if the PSI element does not have any
     *         associated references.
     * @see com.intellij.psi.search.searches.ReferencesSearch
     */
    override fun getReference(): PsiReference = reference

    /**
     * Returns all references from this PSI element to other PSI elements. An element can
     * have multiple references when, for example, the element is a string literal containing
     * multiple sub-strings which are valid full-qualified class names. If an element
     * contains only one text fragment which acts as a reference but the reference has
     * multiple possible targets, {@link PsiPolyVariantReference} should be used instead
     * of returning multiple references.
     * <p/>
     * Actually, it's preferable to call {@link PsiReferenceService#getReferences} instead
     * as it allows adding references by plugins when the element implements {@link ContributedReferenceHost}.
     *
     * Returns the array of references, or an empty array if the element has no associated
     *         references.
     * @see PsiReferenceService#getReferences
     * @see com.intellij.psi.search.searches.ReferencesSearch
     */
    override fun getReferences(): Array<PsiReference> = arrayOf(reference)

    /**
     * Passes the declarations contained in this PSI element and its children
     * for processing to the specified scope processor.
     *
     * @param processor  the processor receiving the declarations.
     * @param lastParent the child of this element has been processed during the previous
     *                   step of the tree up walk (declarations under this element do not need
     *                   to be processed again)
     * @param place      the original element from which the tree up walk was initiated.
     * Returns true if the declaration processing should continue or false if it should be stopped.
     */
    override fun processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean = false

    /**
     * Returns the element which should be used as the parent of this element in a tree up
     * walk during a resolve operation. For most elements, this returns {@code getParent()},
     * but the context can be overridden for some elements like code fragments (see
     * {@link PsiElementFactory#createCodeBlockCodeFragment(String, PsiElement, boolean)}).
     *
     * Returns the resolve context element.
     */
    override fun getContext(): PsiElement? = null

    /**
     * Checks if an actual source or class file corresponds to the element. Non-physical elements include,
     * for example, PSI elements created for the watch expressions in the debugger.
     * Non-physical elements do not generate tree change events.
     * Also, {@link PsiDocumentManager#getDocument(PsiFile)} returns null for non-physical elements.
     * Not to be confused , {@link FileViewProvider#isPhysical()}.
     *
     * Returns true if the element is physical, false otherwise.
     */
    override fun isPhysical(): Boolean = true

    /**
     * Returns the scope in which the declarations for the references in this PSI element are searched.
     *
     * Returns the resolve scope instance.
     */
    override fun getResolveScope(): GlobalSearchScope = containingFile.resolveScope

    /**
     * Returns the scope in which references to this element are searched.
     *
     * Returns the search scope instance.
     * @see { @link com.intellij.psi.search.PsiSearchHelper#getUseScope(PsiElement)}
     */
    override fun getUseScope(): SearchScope = containingFile.resolveScope

    /**
     * Returns the AST node corresponding to the element.
     *
     * Returns the AST node instance.
     */
    override fun getNode(): ASTNode? = null

    /**
     * toString() should never be presented to the user.
     */
    override fun toString(): String = "Name : $name at offset $start to $end in $project"

    /**
     * This method shouldn't be called by clients directly, because there are no guarantees of it being symmetric.
     * It's called by {@link PsiManager#areElementsEquivalent(PsiElement, PsiElement)} internally, which clients should invoke instead.<p/>
     *
     * Implementations of this method should return {@code true} if the parameter is resolve-equivalent to {@code this}, i.e. it represents
     * the same entity from the language perspective. See also {@link PsiManager#areElementsEquivalent(PsiElement, PsiElement)} documentation.
     */
    override fun isEquivalentTo(another: PsiElement): Boolean = this == another

    override fun getName(): String {
        return name
    }

    override fun getIcon(flags: Int): Icon? = null

    override fun getProject(): Project {
        return project
    }

    override fun getNameIdentifier(): PsiElement = this

    override fun setName(name: String): PsiElement {
        this.name = name
        return this
    }

    override fun <T> putUserData(key: Key<T>, value: T?): Unit {
        var control = true
        while (control) {
            val map = getUserMap()
            val newMap = if (value == null) map.minus(key) else map.plus(key, value)
            if ((newMap === map) || changeUserMap(map, newMap)) control = false
        }
    }

    private fun changeUserMap(oldMap: KeyFMap, newMap: KeyFMap): Boolean = updater.compareAndSet(this, oldMap, newMap)

    private fun getUserMap(): KeyFMap = myUserMap

    override fun <T> getCopyableUserData(key: Key<T>): T? {
        val map = getUserData(COPYABLE_USER_MAP_KEY)
        return map?.get(key)
    }

    override fun <T> getUserData(key: Key<T>): T? {
        var t = getUserMap().get(key)
        if (t == null && key is KeyWithDefaultValue) t = putUserDataIfAbsent(key, key.defaultValue)
        return t
    }

    private fun <T> putUserDataIfAbsent(key: Key<T>, value: T): T? {
        while (true) {
            val map = getUserMap()
            val oldValue = map.get(key)
            if (oldValue != null) return oldValue
            val newMap = map.plus(key, value)
            if ((newMap === map) || changeUserMap(map, newMap)) return value
        }
    }

    override fun <T> putCopyableUserData(key: Key<T>, value: T?): Unit {
        var control = true
        while (control) {
            val map = getUserMap()
            val copyableMap = map.get(COPYABLE_USER_MAP_KEY) ?: KeyFMap.EMPTY_MAP
            val newCopyableMap = if (value == null) copyableMap.minus(key) else copyableMap.plus(key, value)
            val newMap = if (newCopyableMap.isEmpty) map.minus(COPYABLE_USER_MAP_KEY) else map.plus(COPYABLE_USER_MAP_KEY, newCopyableMap)
            if ((newMap === map) || changeUserMap(map, newMap)) control = false
        }
    }

    fun <T> replace(key: Key<T>, oldValue: T?, newValue: T?): Boolean {
        while (true) {
            val map = getUserMap()
            if (map.get(key) != oldValue) {
                return false
            } else {
                val newMap = if (newValue == null) map.minus(key) else map.plus(key, newValue)
                if ((newMap === map) || changeUserMap(map, newMap)) return true
            }
        }
    }

    fun copyCopyableDataTo(clone: UserDataHolderBase): Unit {
        clone.putUserData(COPYABLE_USER_MAP_KEY, getUserData(COPYABLE_USER_MAP_KEY))
    }

    fun isUserDataEmpty(): Boolean = getUserMap().isEmpty

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = name

        override fun getLocationString(): String = containingFile.name

        override fun getIcon(unused: Boolean): Icon? = if (unused) null else null //iconProvider.getIcon(LSPPsiElement.this)
    }

    override fun navigate(requestFocus: Boolean): Unit {
        val editor = FileUtils.psiFileToEditor(containingFile)
        if (editor == null) {
            val descriptor = OpenFileDescriptor(getProject(), containingFile.virtualFile, textOffset)
            ApplicationUtils.invokeLater { ApplicationUtils.writeAction { FileEditorManager.getInstance(getProject()).openTextEditor(descriptor, false) } }
        }
    }

    /**
     * Returns the file containing the PSI element.
     *
     * Returns the file instance, or null if the PSI element is not contained in a file (for example,
     *         the element represents a package or directory).
     * @throws PsiInvalidElementAccessException
     * if this element is invalid
     */
    override fun getContainingFile(): PsiFile = file

    /**
     * Returns the offset in the file to which the caret should be placed
     * when performing the navigation to the element. (For classes implementing
     * {@link PsiNamedElement}, this should return the offset in the file of the
     * name identifier.)
     *
     * Returns the offset of the PSI element.
     */
    override fun getTextOffset(): Int = start

    override fun canNavigateToSource(): Boolean = true

    override fun canNavigate(): Boolean = true

    private fun clearUserData(): Unit {
        setUserMap(KeyFMap.EMPTY_MAP)
    }

    private fun setUserMap(map: KeyFMap): Unit {
        myUserMap = map
    }
}