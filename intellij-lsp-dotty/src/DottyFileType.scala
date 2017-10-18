import javax.swing.Icon

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile

object DottyFileType {
  val instance = new DottyFileType
}
class DottyFileType extends LanguageFileType(DottyLanguage.INSTANCE) with FileTypeIdentifiableByVirtualFile {

  override def isMyFileType(file: VirtualFile): Boolean = getDefaultExtension == file.getExtension

  override def getName: String = "Dotty"

  override def getDescription: String = "Dotty files"

  override def getIcon: Icon = AllIcons.Icon_small

  override def getDefaultExtension: String = "scala"
}
