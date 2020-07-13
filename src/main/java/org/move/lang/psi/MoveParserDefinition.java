package org.move.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.move.MoveLanguage;
import org.move.lang.MoveFlexAdapter;
import org.move.lang.MoveParser;
import org.move.lang.MoveTypes;

public class MoveParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(MoveLanguage.INSTANCE);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new MoveFlexAdapter();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new MoveParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return MoveTypes.Factory.createElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return null;
    }
}
