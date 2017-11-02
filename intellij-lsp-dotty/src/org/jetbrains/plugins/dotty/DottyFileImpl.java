package org.jetbrains.plugins.dotty;

import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaFileImpl;

public class DottyFileImpl extends ScalaFileImpl implements DottyFile {

    public DottyFileImpl(@NotNull FileViewProvider provider) {
        super(provider, DottyFileType.INSTANCE);
    }

    @Override
    public String toString() {
        return "DottyFile: " + getName();
    }

}