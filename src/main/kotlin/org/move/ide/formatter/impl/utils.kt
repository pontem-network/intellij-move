package org.move.ide.formatter.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet.orSet
import org.move.lang.MvElementTypes.*
import org.move.lang.MvFile
import org.move.lang.core.psi.*
import com.intellij.psi.tree.TokenSet.create as ts


val UNARY_OPS = ts(MINUS, MUL, EXCL, AND)
val BINARY_OPS = ts(
    PLUS, MINUS, MUL, DIV, MODULO,
    OR, AND, OR_OR, AND_AND,
    EQ, EQ_EQ, NOT_EQ,
)
val ONE_LINE_ITEMS = ts(IMPORT_STATEMENT, CONST_DEF)

val PAREN_DELIMITED_BLOCKS = ts(
    PARENS_EXPR, TUPLE_PAT, TUPLE_TYPE, TUPLE_LIT_EXPR,
    FUNCTION_PARAMETER_LIST, CALL_ARGUMENT_LIST
)
val ANGLE_DELIMITED_BLOCKS = ts(TYPE_PARAMETER_LIST, TYPE_ARGUMENT_LIST)

val STRUCT_LITERAL_BLOCKS = ts(STRUCT_PAT_FIELDS_BLOCK, STRUCT_LIT_FIELDS_BLOCK)
val DEF_BLOCKS = ts(
    SCRIPT_BLOCK, ADDRESS_BLOCK, MODULE_BLOCK, CODE_BLOCK, CODE_BLOCK_EXPR, SPEC_BLOCK, STRUCT_FIELDS_DEF_BLOCK
)
val BLOCK_LIKE = orSet(STRUCT_LITERAL_BLOCKS, DEF_BLOCKS)

val DELIMITED_BLOCKS = orSet(PAREN_DELIMITED_BLOCKS, ANGLE_DELIMITED_BLOCKS, BLOCK_LIKE)

fun ASTNode?.isWhitespaceOrEmpty() = this == null || textLength == 0 || elementType == TokenType.WHITE_SPACE

val PsiElement.isTopLevelItem: Boolean
    get() = (this is MvModuleDef || this is MvAddressDef || this is MvScriptDef) && parent is MvFile

val PsiElement.isModuleItem: Boolean
    get() = this is MvFunction || this is MvConstDef || this is MvStruct_ || this is MvImportStatement

val PsiElement.isDeclarationItem: Boolean
    get() = (this is MvModuleDef && parent is MvAddressBlock) || this.isModuleItem

val PsiElement.isStatement: Boolean
    get() = this is MvStatement && parent is MvCodeBlock

val PsiElement.isStatementOrExpr: Boolean
    get() = this is MvStatement || this is MvExpr && parent is MvCodeBlock

val ASTNode.isDelimitedBlock: Boolean
    get() = elementType in DELIMITED_BLOCKS

fun ASTNode.isDelimiterOfCurrentBlock(parent: ASTNode?): Boolean {
    if (parent == null) return false
    val parentType = parent.elementType
    return when (elementType) {
        L_BRACE, R_BRACE -> parentType in BLOCK_LIKE
        L_PAREN, R_PAREN -> parentType in PAREN_DELIMITED_BLOCKS
        LT, GT -> parentType in ANGLE_DELIMITED_BLOCKS
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
//            CommaList(STRUCT_LITERAL_BODY, LBRACE, RBRACE) { it.elementType == STRUCT_LIT_FIELD },
//            CommaList(ENUM_BODY, LBRACE, RBRACE) { it.elementType == ENUM_VARIANT },
//            CommaList(USE_GROUP, LBRACE, RBRACE) { it.elementType == USE_SPECK },
//
//            CommaList(TUPLE_FIELDS, LPAREN, RPAREN) { it.elementType == TUPLE_FIELD_DECL },
//            CommaList(VALUE_PARAMETER_LIST, LPAREN, RPAREN) { it.elementType == VALUE_PARAMETER },
//            CommaList(VALUE_ARGUMENT_LIST, LPAREN, RPAREN) { it is RsExpr }
//        )
//    }
//}
