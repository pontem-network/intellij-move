package org.move;

import com.intellij.lang.Language;


public class MoveLanguage extends Language {
    public static final MoveLanguage INSTANCE = new MoveLanguage();

    protected MoveLanguage() {
        super("Move");
    }
}
