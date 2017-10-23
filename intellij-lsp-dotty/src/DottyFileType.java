import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class DottyFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile {

    public static final DottyFileType INSTANCE = new DottyFileType();

    public DottyFileType() {
        super(DottyLanguage.INSTANCE);
    }

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        return Objects.equals(getDefaultExtension(), file.getExtension());
    }

    @NotNull
    @Override
    public String getName() {
        return "Dotty";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Dotty files";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "scala";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return AllIcons.Icon_small;
    }
}
