package org.move.ide.formatter.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import org.move.lang.MvElementTypes.*
import com.intellij.psi.tree.TokenSet.create as ts

val DELIMITED_BLOCKS = ts(SCRIPT_BLOCK, ADDRESS_BLOCK, MODULE_BLOCK, CODE_BLOCK)

fun ASTNode?.isWhitespaceOrEmpty() = this == null || textLength == 0 || elementType == TokenType.WHITE_SPACE

val ASTNode.isDelimitedBlock: Boolean
    get() = elementType in DELIMITED_BLOCKS

//fun ASTNode.isBlockDelim(parent: ASTNode?): Boolean {
//    if (parent == null) return false
//    val parentType = parent.elementType
//    return when (elementType) {
//        LBRACE, RBRACE -> parentType in BRACE_DELIMITED_BLOCKS || parent.isFlatBraceBlock
//        LBRACK, RBRACK -> parentType in BRACK_LISTS
//        LPAREN, RPAREN -> parentType in PAREN_LISTS || parentType == PAT_TUPLE_STRUCT
//        LT, GT -> parentType in ANGLE_LISTS
//        OR -> parentType == VALUE_PARAMETER_LIST && parent.treeParent?.elementType == LAMBDA_EXPR
//        else -> false
//    }
//}
