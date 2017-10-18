import com.github.gtache.ServerDefinitionExtensionPoint;

public class DottyLanguageExtension extends ServerDefinitionExtensionPoint {

    public static DottyLanguageExtension INSTANCE;

    public DottyLanguageExtension() {
        super("scala", "ch.epfl.lamp:dotty-language-server_0.3:0.3.0-RC2", "dotty.tools.languageserver.Main", new String[]{"-stdio"});
    }
}
