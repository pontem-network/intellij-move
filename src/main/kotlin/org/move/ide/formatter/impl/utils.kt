package org.move.ide.formatter.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import org.move.lang.MvElementTypes.*
import com.intellij.psi.tree.TokenSet.create as ts

val BLOCK_LIKE = ts(SCRIPT_BLOCK, ADDRESS_BLOCK, MODULE_BLOCK, CODE_BLOCK, STRUCT_FIELDS)

fun ASTNode?.isWhitespaceOrEmpty() = this == null || textLength == 0 || elementType == TokenType.WHITE_SPACE

val ASTNode.isDelimitedBlock: Boolean
    get() = elementType in BLOCK_LIKE

fun ASTNode.isBlockDelim(parent: ASTNode?): Boolean {
    if (parent == null) return false
    val parentType = parent.elementType
    return when (elementType) {
        L_BRACE, R_BRACE -> parentType in BLOCK_LIKE // || parent.isFlatBraceBlock
//        LBRACK, RBRACK -> parentType in BRACK_LISTS
//        LPAREN, RPAREN -> parentType in PAREN_LISTS || parentType == PAT_TUPLE_STRUCT
//        LT, GT -> parentType in ANGLE_LISTS
//        OR -> parentType == VALUE_PARAMETER_LIST && parent.treeParent?.elementType == LAMBDA_EXPR
        else -> false
    }
}
