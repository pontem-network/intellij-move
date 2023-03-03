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
import org.move.lang.core.MvTokenType
import org.move.lang.core.lexer.createMoveLexer
import org.move.lang.core.stubs.impl.MvFileStub
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
        return tokenSetOf(EOL_COMMENT, BLOCK_COMMENT, EOL_DOC_COMMENT)
    }

    override fun getStringLiteralElements(): TokenSet {
        // hex string is not included, as it's basically vector<u8> and not string
        return tokenSetOf(MvElementTypes.BYTE_STRING_LITERAL)
    }

    override fun createElement(node: ASTNode): PsiElement {
        return MvElementTypes.Factory.createElement(node)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return MoveFile(viewProvider)
    }

    companion object {
        val FILE = MvFileStub.Type

        @JvmField
        val BLOCK_COMMENT = MvTokenType("BLOCK_COMMENT")

        @JvmField
        val EOL_COMMENT = MvTokenType("EOL_COMMENT")

        @JvmField
        val EOL_DOC_COMMENT = MvTokenType("EOL_DOC_COMMENT")

        /**
         * Should be increased after any change of lexer rules
         */
        const val LEXER_VERSION: Int = 1

        /**
         * Should be increased after any change of parser rules
         */
        const val PARSER_VERSION: Int = LEXER_VERSION + 45
    }
}
