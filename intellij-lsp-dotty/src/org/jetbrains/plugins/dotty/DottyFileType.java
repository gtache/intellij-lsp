package org.jetbrains.plugins.dotty;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.ScalaFileType;
import org.jetbrains.plugins.scala.icons.Icons;

import javax.swing.*;
import java.util.Objects;

public class DottyFileType extends ScalaFileType {

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
        return Icons.FILE;
    }
}
