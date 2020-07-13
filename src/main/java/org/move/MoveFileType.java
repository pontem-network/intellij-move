package org.move;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MoveFileType extends LanguageFileType {
    public static final MoveFileType INSTANCE = new MoveFileType();

    protected MoveFileType() {
        super(MoveLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Move";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Move Language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "move";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return MoveIcons.FILE;
    }
}
