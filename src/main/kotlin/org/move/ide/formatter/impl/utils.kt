package org.move.ide.formatter.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet.orSet
import org.move.lang.MvElementTypes.*
import com.intellij.psi.tree.TokenSet.create as ts


val UNARY_OPS = ts(MINUS, MUL, EXCL, AND)
val BINARY_OPS = ts(PLUS, MINUS, MUL, DIV, MODULO,
                    OR, AND, OR_OR, AND_AND,
                    EQ, EQ_EQ)

val PAREN_DELIMITED_BLOCKS = ts(PARENS_EXPR, TUPLE_PAT, TUPLE_TYPE, TUPLE_LITERAL_EXPR,
                                FUNCTION_PARAMS, CALL_ARGUMENTS)
val ANGLE_DELIMITED_BLOCKS = ts(TYPE_PARAMETER_LIST, TYPE_ARGUMENT_LIST)

val BLOCK_LIKE = ts(SCRIPT_BLOCK, ADDRESS_BLOCK, MODULE_BLOCK, CODE_BLOCK, STRUCT_FIELDS)

val DELIMITED_BLOCKS = orSet(PAREN_DELIMITED_BLOCKS, ANGLE_DELIMITED_BLOCKS, BLOCK_LIKE)

fun ASTNode?.isWhitespaceOrEmpty() = this == null || textLength == 0 || elementType == TokenType.WHITE_SPACE

val ASTNode.isDelimitedBlock: Boolean
    get() = elementType in DELIMITED_BLOCKS

fun ASTNode.isBlockDelim(parent: ASTNode?): Boolean {
    if (parent == null) return false
    val parentType = parent.elementType
    return when (elementType) {
        L_BRACE, R_BRACE -> parentType in BLOCK_LIKE // || parent.isFlatBraceBlock
//        LBRACK, RBRACK -> parentType in BRACK_LISTS
        L_PAREN, R_PAREN -> parentType in PAREN_DELIMITED_BLOCKS // || parentType == PAT_TUPLE_STRUCT
        LT, GT -> parentType in ANGLE_DELIMITED_BLOCKS
//        OR -> parentType == VALUE_PARAMETER_LIST && parent.treeParent?.elementType == LAMBDA_EXPR
        else -> false
    }
}

//class CommaList(
//    val list: IElementType,
//    val openingBrace: IElementType,
//    val closingBrace: IElementType,
//    val isElement: (PsiElement) -> Boolean
//) {
//    val needsSpaceBeforeClosingBrace: Boolean get() = closingBrace == R_BRACE // && list != USE_GROUP
//
//    override fun toString(): String = "CommaList($list)"
//
//    companion object {
//        fun forElement(elementType: IElementType): CommaList? {
//            return ALL.find { it.list == elementType }
//        }
//
//        private val ALL = listOf(
//            CommaList(BLOCK_FIELDS, LBRACE, RBRACE) { it.elementType == NAMED_FIELD_DECL },
//            CommaList(STRUCT_LITERAL_BODY, LBRACE, RBRACE) { it.elementType == STRUCT_LITERAL_FIELD },
//            CommaList(ENUM_BODY, LBRACE, RBRACE) { it.elementType == ENUM_VARIANT },
//            CommaList(USE_GROUP, LBRACE, RBRACE) { it.elementType == USE_SPECK },
//
//            CommaList(TUPLE_FIELDS, LPAREN, RPAREN) { it.elementType == TUPLE_FIELD_DECL },
//            CommaList(VALUE_PARAMETER_LIST, LPAREN, RPAREN) { it.elementType == VALUE_PARAMETER },
//            CommaList(VALUE_ARGUMENT_LIST, LPAREN, RPAREN) { it is RsExpr }
//        )
//    }
//}
