import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.impl.source.tree.OwnBufferLeafPsiElement;
import org.jetbrains.annotations.NotNull;

public class DottyTextImpl extends OwnBufferLeafPsiElement {
    protected DottyTextImpl(@NotNull CharSequence text) {
        super(DottyParserDefinition.DOTTY_TOKEN_TYPE, text);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        visitor.visitElement(this);
    }

    public String toString() {
        return "DottyText";
    }
}
