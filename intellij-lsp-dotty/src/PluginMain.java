import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

public class PluginMain implements ProjectComponent {

    public PluginMain(Project project) {
        DottyLanguageExtension$.MODULE$.register();
        Logger.getInstance(PluginMain.class).info("Instantiated Dotty");
    }

    @Override
    public void initComponent() {

    }

}
