package org.move.lang;

import com.intellij.lexer.FlexAdapter;

public class MoveFlexAdapter extends FlexAdapter {
    public MoveFlexAdapter() {
        super(new _MoveLexer(null));
    }
}
