import com.intellij.lang.Language;

public class DottyLanguage extends Language {
    public static final DottyLanguage INSTANCE = new DottyLanguage();
    protected DottyLanguage() {
        super("dotty");
    }
}
