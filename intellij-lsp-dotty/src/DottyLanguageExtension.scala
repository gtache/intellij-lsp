import com.github.gtache.{ServerDefinitionExtensionPoint, ServerDefinitionExtensionPointArtifact}


object DottyLanguageExtension extends ServerDefinitionExtensionPointArtifact("scala", "ch.epfl.lamp:dotty-language-server_0.3:0.3.0-RC2", "dotty.tools.languageserver.Main", Array[String]("-stdio")) {
  def register(): Unit = ServerDefinitionExtensionPoint.register(this)
}