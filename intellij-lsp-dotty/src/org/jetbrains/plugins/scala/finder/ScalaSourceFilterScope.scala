package org.jetbrains.plugins.scala.finder

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.{FileTypeManager, StdFileTypes}
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.{ProjectFileIndex, ProjectRootManager}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope.{getScopeRestrictedByFileTypes, projectScope}
import com.intellij.psi.search.searches.{MethodReferencesSearch, ReferencesSearch}
import com.intellij.psi.search.{GlobalSearchScope, LocalSearchScope, SearchScope}
import org.jetbrains.plugins.scala.lang.psi.ElementScope
import org.jetbrains.plugins.scala.{ScalaFileType, ScalaLanguage}

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.02.2010
  */
class ScalaSourceFilterScope(implicit elementScope: ElementScope) extends SourceFilterScope {
  def this(scope: GlobalSearchScope, project: Project) =
    this()(ElementScope(project, scope))

  override protected def isValid(file: VirtualFile): Boolean =
    super.isValid(file) && (FileTypeManager.getInstance().isFileOfType(file, ScalaFileType.INSTANCE) ||
      StdFileTypes.CLASS.getDefaultExtension == file.getExtension && myIndex.isInLibraryClasses(file)) //CHANGED
}

object ScalaSourceFilterScope {
  def apply(search: ReferencesSearch.SearchParameters): SearchScope =
    updateScope(search.getProject)(search.getEffectiveSearchScope)

  private def updateScope(project: Project): SearchScope => SearchScope = {
    case global: GlobalSearchScope =>
      implicit val elementScope = ElementScope(project, global)
      new ScalaSourceFilterScope
    case local: LocalSearchScope =>
      val filtered = local.getScope.filter(_.getLanguage.isKindOf(ScalaLanguage.INSTANCE))
      val displayName = local.getDisplayName + " in scala"
      new LocalSearchScope(filtered, displayName, local.isIgnoreInjectedPsi)
    case scope => scope
  }

  def apply(search: MethodReferencesSearch.SearchParameters): SearchScope =
    updateScope(search.getProject)(search.getEffectiveSearchScope)

  def apply(project: Project, scope: SearchScope): SearchScope = updateScope(project)(scope)
}

class SourceFilterScope protected(implicit elementScope: ElementScope) extends GlobalSearchScope(elementScope.project) {
  protected val myIndex: ProjectFileIndex = ProjectRootManager.getInstance(elementScope.project).getFileIndex
  private val myDelegate = elementScope.scope

  override def contains(file: VirtualFile): Boolean =
    (myDelegate == null || myDelegate.contains(file)) && isValid(file)

  protected def isValid(file: VirtualFile): Boolean =
    myIndex.isInSourceContent(file)

  override def compare(file1: VirtualFile, file2: VirtualFile): Int =
    if (myDelegate != null) myDelegate.compare(file1, file2) else 0

  override def isSearchInModuleContent(aModule: Module): Boolean =
    myDelegate == null || myDelegate.isSearchInModuleContent(aModule)

  override def isSearchInLibraries: Boolean =
    myDelegate == null || myDelegate.isSearchInLibraries
}

object SourceFilterScope {
  def apply(project: Project): GlobalSearchScope =
    apply(project, projectScope(project))

  def apply(project: Project, scope: GlobalSearchScope): GlobalSearchScope = {
    val updatedScope = getScopeRestrictedByFileTypes(scope, ScalaFileType.INSTANCE, JavaFileType.INSTANCE)
    implicit val elementScope = ElementScope(project, updatedScope)
    new SourceFilterScope
  }
}
