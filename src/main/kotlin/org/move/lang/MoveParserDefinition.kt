package org.move.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.move.lang.core.lexer.createMoveLexer
//import org.move.lang.core.stubs.MoveFileStub
import org.move.lang.core.tokenSetOf

class MoveParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer {
        return createMoveLexer()
    }

    override fun createParser(project: Project): PsiParser {
        return MoveParser()
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet {
        return tokenSetOf(MoveElementTypes.LINE_COMMENT, MoveElementTypes.BLOCK_COMMENT)
    }

    override fun getStringLiteralElements(): TokenSet {
        // hex string is not included, as it's basically vector<u8> and not string
        return tokenSetOf(MoveElementTypes.BYTE_STRING_LITERAL)
    }

    override fun createElement(node: ASTNode): PsiElement {
        return MoveElementTypes.Factory.createElement(node)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return MoveFile(viewProvider)
    }

    companion object {
        val FILE = IFileElementType(MoveLanguage)
    }
}