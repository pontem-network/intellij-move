package org.move.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.move.MoveFileType;
import org.move.MoveLanguage;

public class MoveFile extends PsiFileBase {
    protected MoveFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, MoveLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return MoveFileType.INSTANCE;
    }
}
