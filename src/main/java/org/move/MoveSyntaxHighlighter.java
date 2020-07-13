package org.move;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.move.lang.MoveFlexAdapter;
import org.move.lang.MoveTypes;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class MoveSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey KEYWORD =
            createTextAttributesKey("MOVE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
//    public static final TextAttributesKey VALUE =
//            createTextAttributesKey("SIMPLE_VALUE", DefaultLanguageHighlighterColors.STRING);
//    public static final TextAttributesKey COMMENT =
//            createTextAttributesKey("SIMPLE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
//    public static final TextAttributesKey BAD_CHARACTER =
//            createTextAttributesKey("SIMPLE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new MoveFlexAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(MoveTypes.LET) || tokenType.equals(MoveTypes.MUT)) {
            return new TextAttributesKey[]{KEYWORD};
        } else {
            return new TextAttributesKey[0];
        }
    }
}
