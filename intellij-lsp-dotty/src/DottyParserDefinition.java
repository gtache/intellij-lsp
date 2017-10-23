import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.EmptyLexer;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiPlainTextFileImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class DottyParserDefinition implements ParserDefinition {
    public static final IElementType DOTTY_TOKEN_TYPE = new IElementType("Dotty", DottyLanguage.INSTANCE);

    private static final IFileElementType DOTTY_FILE_ELEMENT_TYPE = new IFileElementType(DottyLanguage.INSTANCE) {
        @Override
        public ASTNode parseContents(ASTNode chameleon) {
            CharSequence chars = chameleon.getChars();
            return ASTFactory.leaf(DOTTY_TOKEN_TYPE, chars);
        }
    };

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new EmptyLexer();
    }

    @Override
    public PsiParser createParser(Project project) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public IFileElementType getFileNodeType() {
        return DOTTY_FILE_ELEMENT_TYPE;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return null;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new PsiPlainTextFileImpl(viewProvider); //TODO DottyFileImpl error
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
