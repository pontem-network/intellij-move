package org.move.lang.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.move.MoveLanguage;

public class MvTokenType extends IElementType {
    public MvTokenType(@NotNull String debugName) {
        super(debugName, MoveLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "MoveTokenType." + super.toString();
    }
}
