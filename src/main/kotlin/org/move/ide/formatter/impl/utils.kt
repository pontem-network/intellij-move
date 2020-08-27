package org.move.ide.formatter.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
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
