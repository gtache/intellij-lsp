import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import org.jetbrains.annotations.NotNull


class DottyFileTypeFactory extends FileTypeFactory {
  override def createFileTypes(consumer: FileTypeConsumer): Unit = {
    consumer.consume(DottyFileType.INSTANCE, "scala")
  }
}