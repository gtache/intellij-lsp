package org.jetbrains.plugins.scala.worksheet.ammonite

import java.io.File

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.{OrderRootType, ProjectRootManager}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi._
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.util.containers.ContainerUtilRt
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReferenceElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportExpr
import org.jetbrains.plugins.scala.lang.psi.api.{FileDeclarationsHolder, ScalaFile}
import org.jetbrains.plugins.scala.lang.psi.impl.{ScalaFileImpl, ScalaPsiElementFactory}
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.util.ScalaUtil

/**
  * User: Dmitry.Naydanov
  * Date: 01.08.17.
  */
object AmmoniteUtil {
  val AMMONITE_EXTENSION = "sc"

  private val DEFAULT_VERSION = "2.12"
  private val ROOT_FILE = "$file"
  private val ROOT_EXEC = "$exec"
  private val ROOT_IVY = "$ivy"
  private val PARENT_FILE = "^"

  private val DEFAULT_IMPORTS = Seq("ammonite.main.Router._") //todo more default imports ? 

  def executeImplicitImportsDeclarations(processor: PsiScopeProcessor, file: FileDeclarationsHolder, state: ResolveState): Boolean = {
    file match {
      case ammoniteFile: ScalaFile if isAmmoniteFile(ammoniteFile) =>
        DEFAULT_IMPORTS.foreach {
          imp =>
            val importStmt = ScalaPsiElementFactory.createImportFromText(s"import $imp")(ammoniteFile.projectContext)
            importStmt.processDeclarations(processor, state, null, ammoniteFile)
        }
      case _ =>
    }

    true
  }

  def isAmmoniteFile(file: ScalaFile): Boolean = {
    ScalaUtil.findVirtualFile(file) match {
      case Some(vFile) => isAmmoniteFile(vFile, file.getProject)
      case _ => false
    }
  }

  def isAmmoniteFile(virtualFile: VirtualFile, project: Project): Boolean = {
    virtualFile.getExtension == AMMONITE_EXTENSION && (ScalaProjectSettings.getInstance(project).getScFileMode match {
      case ScalaProjectSettings.ScFileMode.Ammonite => true
      case ScalaProjectSettings.ScFileMode.Worksheet => false
      case ScalaProjectSettings.ScFileMode.Auto =>
        ProjectRootManager.getInstance(project).getFileIndex.isUnderSourceRootOfType(virtualFile, ContainerUtilRt.newHashSet(JavaSourceRootType.TEST_SOURCE))
      case _ => false
    })
  }

  /*
  Resolves $file imports
   */
  def scriptResolveQualifier(refElement: ScStableCodeReferenceElement): Option[PsiFileSystemItem] = {
    def scriptResolveNoQualifier(refElement: ScStableCodeReferenceElement): Option[PsiDirectory] =
      refElement.getContainingFile match {
        case scalaFile: ScalaFileImpl if isAmmoniteFile(scalaFile) =>
          if (refElement.getText == ROOT_FILE || refElement.getText == ROOT_EXEC) {
            val dir = scalaFile.getContainingDirectory

            if (dir != null) Option(scalaFile.getContainingDirectory) else {
              Option(scalaFile.getOriginalFile).flatMap(file => Option(file.getContainingDirectory))
            }
          } else None
        case _ => None
      }

    refElement.qualifier match {
      case Some(q) =>
        scriptResolveQualifier(q) match {
          case Some(d) =>
            refElement.refName match {
              case PARENT_FILE => Option(d.getParent)
              case other =>
                d match {
                  case dir: PsiDirectory =>
                    Option(dir findFile s"$other.$AMMONITE_EXTENSION").orElse(Option(dir findSubdirectory other))
                  case _ => None
                }
            }
          case a@None => a
        }
      case None => scriptResolveNoQualifier(refElement)
    }
  }

  def isAmmoniteSpecificImport(expr: ScImportExpr): Boolean = {
    val txt = expr.getText
    txt.startsWith(ROOT_EXEC) || txt.startsWith(ROOT_FILE) || txt.startsWith(ROOT_IVY)
  }

  def getDefaultCachePath: String = System.getProperty("user.home") + "/.ivy2/cache"

  def getCoursierCachePath: String = System.getProperty("user.home") + "/.coursier/cache/v1"

  def getModuleForFile(virtualFile: VirtualFile, project: Project): Option[Module] =
    Option(ProjectRootManager.getInstance(project).getFileIndex.getModuleForFile(virtualFile))

  private def findFileByPattern(pattern: String, predicate: File => Boolean): Option[File] = {
    abstract class PathPart[T] {
      protected var current: Option[T] = None

      def getCurrent: Option[T] = current

      def hasNext: Boolean = false

      def add(): Boolean

      def reset()
    }

    case class SimplePart(p: String) extends PathPart[String] {
      reset()

      override def add(): Boolean = {
        current = None
        false
      }

      override def reset(): Unit = current = Option(p)
    }

    case class OrPart(ps: Iterable[String]) extends PathPart[String] {
      private var it = ps.iterator
      setCurrent()

      override def add(): Boolean = {
        it.hasNext && {
          setCurrent(); true
        } || {
          current = None; false
        }
      }

      override def hasNext: Boolean = it.hasNext

      override def reset(): Unit = {
        it = ps.iterator
        setCurrent()
      }

      private def setCurrent() {
        current = Option(it.next())
      }
    }

    case class PathIterator(pathParts: Iterable[PathPart[String]]) extends Iterator[File] {
      private var it = pathParts.iterator
      private var currentDigit = pathParts.head
      private var currentVal: Option[String] = Option(gluePath)

      def hasNext: Boolean = currentVal.isDefined

      def next(): File = {
        val c = currentVal.get
        currentVal = None
        advance()
        new File(c)
      }

      private def advance() {
        if (!currentDigit.add()) {
          while (!currentDigit.add() && it.hasNext) currentDigit = it.next()
          if (currentDigit.getCurrent.isEmpty) return
          pathParts.takeWhile(_ != currentDigit).foreach(_.reset())
          currentDigit = pathParts.head
          it = pathParts.iterator
        }

        currentVal = Option(gluePath)
      }

      private def gluePath: String = pathParts.flatMap(_.getCurrent.toList).mkString(File.separator)
    }

    PathIterator {
      pattern.split('/').map {
        part =>
          part.split('|') match {
            case Array(single) => SimplePart(single)
            case multiple => OrPart(multiple)
          }
      }.foldRight(List.empty[PathPart[String]]) {
        case (SimplePart(part), SimplePart(pp) :: tail) =>
          SimplePart(part + File.separator + pp) :: tail
        case (otherPart, list) =>
          otherPart :: list
      }
    }.find(predicate)
  }

  private def getResolveItem(library: Library, project: Project): Option[PsiDirectory] = getLibraryDirs(library, project).headOption

  private def getLibraryDirs(library: Library, project: Project): Array[PsiDirectory] = {
    library.getFiles(OrderRootType.CLASSES).flatMap {
      root => Option(PsiManager.getInstance(project).findDirectory(root))
    }
  }

  case class LibInfo(groupId: String, name: String, version: String, scalaVersion: String)
}
