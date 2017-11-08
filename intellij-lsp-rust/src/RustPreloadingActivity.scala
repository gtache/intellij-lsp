import com.github.gtache.client.languageserver.serverdefinition.{ExeLanguageServerDefinition, LanguageServerDefinition}
import com.github.gtache.utils.Utils
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.progress.ProgressIndicator

class RustPreloadingActivity extends PreloadingActivity {

  override def preload(indicator: ProgressIndicator): Unit = {
    //Assume rls is on Path
    LanguageServerDefinition.register(new ExeLanguageServerDefinition("rs", if (Utils.os == Utils.OS.WINDOWS) "rls.exe" else "rls", Array()))
  }
}
