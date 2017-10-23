import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

public class PluginMain implements ProjectComponent {

    public PluginMain(Project project) {
        Logger.getInstance(PluginMain.class).info("Instantiating Dotty");
    }

    @Override
    public void initComponent() {

    }

}
