package org.move.ide.formatter.impl

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import org.move.ide.formatter.MoveFormatterBlock
import org.move.lang.MoveElementTypes.*
import org.move.lang.core.psi.*

fun MoveFormatterBlock.computeIndent(child: ASTNode): Indent? {
    val parentType = node.elementType
    val parentPsi = node.psi
    val childType = child.elementType
    val childPsi = child.psi
    return when {
        node.isDelimitedBlock -> getNormalIndentIfNotCurrentBlockDelimiter(child, node)
        // do not indent statements
        childPsi.prevSibling == null -> Indent.getNoneIndent()
        //     let a =
        //     92;
        // =>
        //     let a =
        //         92;
        // except if RefExpr as lhs of assignment expr
        childPsi is MoveExpr
                && (parentType == LET_EXPR || parentType == ASSIGNMENT_EXPR || parentType == CONST_DEF) -> Indent.getNormalIndent()
//        if (true)
//            create()
//        else
//            delete()
        parentPsi is MoveIfExpr || parentPsi is MoveElseBlock -> when (childPsi) {
            is MoveInlineBlock -> Indent.getNormalIndent()
            else -> Indent.getNoneIndent()
        }
        parentPsi is MoveSpecExpr -> Indent.getNormalIndent()

        // binary expressions, chain calls
        parentPsi is MoveExpr -> Indent.getContinuationWithoutFirstIndent()

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
