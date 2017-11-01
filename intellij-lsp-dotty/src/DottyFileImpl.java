import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DottyFileImpl extends PsiFileBase {
    private final FileType myFileType;

    protected DottyFileImpl(@NotNull FileViewProvider provider) {
        super(provider, DottyLanguage.INSTANCE);
        myFileType = Objects.equals(provider.getBaseLanguage(), DottyLanguage.INSTANCE) ? provider.getFileType() : DottyFileType.INSTANCE;
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return myFileType;
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        visitor.visitFile(this);
    }

    @Override
    @NotNull
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }
}