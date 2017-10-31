import com.intellij.lang.ASTFactory;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.tree.IElementType;

public class DottyASTFactory extends ASTFactory {

    @Override
    public LeafElement createLeaf(final IElementType type, CharSequence text) {
        return type.equals(DottyParserDefinition.DOTTY_TOKEN_TYPE) ? new DottyTextImpl(text) : null;
    }
}
