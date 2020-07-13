package org.move.lang.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.move.MoveLanguage;

public class MvElementType extends IElementType {
    public MvElementType(@NotNull String debugName) {
        super(debugName, MoveLanguage.INSTANCE);
    }


}
