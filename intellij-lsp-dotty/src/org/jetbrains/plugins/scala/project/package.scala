package org.jetbrains.plugins.scala

import com.intellij.lang.Language
import com.intellij.openapi.module.{ModifiableModuleModel, Module, ModuleManager, ModuleUtilCore}
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots._
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.{Key, UserDataHolder, UserDataHolderEx}
import com.intellij.psi.PsiElement
import com.intellij.util.CommonProcessors.CollectProcessor
import org.jetbrains.plugins.dotty.DottyLanguage
import org.jetbrains.plugins.dotty.lang.psi.types.DottyTypeSystem
import org.jetbrains.plugins.scala.caches.CachesUtil
import org.jetbrains.plugins.scala.lang.psi.types.ScalaTypeSystem
import org.jetbrains.plugins.scala.lang.psi.types.api.TypeSystem

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.matching.Regex

/**
  * @author Pavel Fatin
  */
package object project {

  implicit class ModuleExt(val module: Module) extends AnyVal {
    def hasScala: Boolean =
      true

    def hasDotty: Boolean =
      true

    def libraries: Set[Library] = {
      val collector = new CollectProcessor[Library]()
      OrderEnumerator.orderEntries(module)
        .librariesOnly()
        .forEachLibrary(collector)

      collector.getResults.asScala.toSet
    }

    def attach(library: Library): Unit = {
      val model = modifiableModel
      model.addLibraryEntry(library)
      model.commit()
    }

    def detach(library: Library): Unit = {
      val model = modifiableModel
      val entry = model.findLibraryOrderEntry(library)
      model.removeOrderEntry(entry)
      model.commit()
    }

    def modifiableModel: ModifiableRootModel =
      ModuleRootManager.getInstance(module).getModifiableModel
  }

  implicit class ProjectExt(val project: Project) extends AnyVal {

    def modifiableModel: ModifiableModuleModel =
      manager.getModifiableModel

    private def manager =
      ModuleManager.getInstance(project)

    def hasScala: Boolean =
      modules.exists(_.hasScala)

    private def modules: Seq[Module] =
      manager.getModules.toSeq

    def libraries: Seq[Library] =
      ProjectLibraryTable.getInstance(project).getLibraries.toSeq

    def typeSystem: TypeSystem = {
      if (project.hasDotty) DottyTypeSystem(project)
      else ScalaTypeSystem(project)
    }

    def hasDotty: Boolean = {
      val cached = project.getUserData(CachesUtil.PROJECT_HAS_DOTTY_KEY)
      if (cached != null) cached
      else {
        val result = modulesWithScala.exists(_.hasDotty)
        if (project.isInitialized) {
          project.putUserData(CachesUtil.PROJECT_HAS_DOTTY_KEY, java.lang.Boolean.valueOf(result))
        }
        result
      }
    }

    def modulesWithScala: Seq[Module] =
      modules.filter(_.hasScala)

    def language: Language =
      if (project.hasDotty) DottyLanguage.INSTANCE else ScalaLanguage.INSTANCE
  }

  implicit class UserDataHolderExt(val holder: UserDataHolder) extends AnyVal {
    def getOrUpdateUserData[T](key: Key[T], update: => T): T = {
      Option(holder.getUserData(key)).getOrElse {
        val newValue = update
        holder match {
          case ex: UserDataHolderEx =>
            ex.putUserDataIfAbsent(key, newValue)
          case _ =>
            holder.putUserData(key, newValue)
            newValue
        }
      }
    }
  }


  implicit class ProjectPsiElementExt(val element: PsiElement) extends AnyVal {
    def isInScalaModule: Boolean = module.exists(_.hasScala)

    def module: Option[Module] = Option(ModuleUtilCore.findModuleForPsiElement(element))

    def isInDottyModule: Boolean = module.exists(_.hasDotty)

    @deprecated("legacy code, use scalaLanguageLevelOrDefault", "14.10.14")
    def languageLevel: ScalaLanguageLevel = ScalaLanguageLevel.Scala_2_12

    def scalaLanguageLevelOrDefault: ScalaLanguageLevel = scalaLanguageLevel.getOrElse(ScalaLanguageLevel.Default)

    def scalaLanguageLevel: Option[ScalaLanguageLevel] = Option(ScalaLanguageLevel.Scala_2_12)
  }

  val LibraryVersion: Regex = """(?<=:|-)\d+\.\d+\.\d+[^:\s]*""".r

  val JarVersion: Regex = """(?<=-)\d+\.\d+\.\d+\S*(?=\.jar$)""".r

  val ScalaLibraryName: String = "scala-library"

  val DottyLibraryName: String = "dotty-library"
}
