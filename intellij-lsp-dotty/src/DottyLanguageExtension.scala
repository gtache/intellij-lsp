import com.github.gtache.ServerDefinitionExtensionPoint


object DottyLanguageExtension {
  var INSTANCE: DottyLanguageExtension = null
}

class DottyLanguageExtension() extends ServerDefinitionExtensionPoint("scala", "ch.epfl.lamp:dotty-language-server_0.3:0.3.0-RC2", "dotty.tools.languageserver.Main", Array[String]("-stdio")) {
}