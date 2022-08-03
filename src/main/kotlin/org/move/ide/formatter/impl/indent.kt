package org.move.ide.formatter.impl

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import org.move.ide.formatter.MvFormatterBlock
import org.move.lang.MvElementTypes.*
import org.move.lang.core.psi.*
import org.move.lang.core.tokenSetOf

fun MvFormatterBlock.computeIndent(child: ASTNode): Indent? {
    val nodePsi = node.psi
    val elementType = node.elementType
    val childPsi = child.psi
    return when {
        elementType in tokenSetOf(CODE_BLOCK_EXPR) -> Indent.getNoneIndent()
        elementType in DELIMITED_BLOCKS -> getNormalIndentIfNotCurrentBlockDelimiter(child, node)
        // do not indent statements
        childPsi.prevSibling == null -> Indent.getNoneIndent()
        //     let a =
        //     92;
        // =>
        //     let a =
        //         92;
        // except if RefExpr as lhs of assignment expr
//        childPsi is MvExpr
//                && (parentType == LET_EXPR || parentType == ASSIGNMENT_EXPR || parentType == CONST_DEF) -> Indent.getNormalIndent()
        childPsi is MvExpr
                && nodePsi is MvInitializer -> Indent.getNormalIndent()
//        if (true)
//            create()
//        else
//            delete()
        nodePsi is MvIfExpr || nodePsi is MvElseBlock -> when (childPsi) {
            is MvInlineBlock -> Indent.getNormalIndent()
            else -> Indent.getNoneIndent()
        }
//        parentPsi is MvSpecBlockExpr -> Indent.getNormalIndent()

        // binary expressions, chain calls
        // no indent on it's own, use parent indent
        nodePsi is MvExpr -> Indent.getIndent(Indent.Type.NONE, true, true)

        else -> Indent.getNoneIndent()
    }
}

fun getNormalIndentIfNotCurrentBlockDelimiter(child: ASTNode, parent: ASTNode): Indent =
    if (child.isDelimiterOfCurrentBlock(parent)) {
        Indent.getNoneIndent()
    } else {
        if (parent.elementType == ADDRESS_BLOCK) {
            Indent.getNoneIndent()
        } else {
            Indent.getNormalIndent()
        }
    }
