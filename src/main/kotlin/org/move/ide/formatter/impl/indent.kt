package org.move.ide.formatter.impl

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import org.move.ide.formatter.MvFormatterBlock
import org.move.lang.MvElementTypes.ADDRESS_BLOCK
import org.move.lang.core.psi.*

fun MvFormatterBlock.computeIndent(child: ASTNode): Indent? {
//    val parentType = node.elementType
    val parentPsi = node.psi
//    val childType = child.elementType
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
//        childPsi is MvExpr
//                && (parentType == LET_EXPR || parentType == ASSIGNMENT_EXPR || parentType == CONST_DEF) -> Indent.getNormalIndent()
        childPsi is MvExpr
                && parentPsi is MvInitializer -> Indent.getNormalIndent()
//        if (true)
//            create()
//        else
//            delete()
        parentPsi is MvIfExpr || parentPsi is MvElseBlock -> when (childPsi) {
            is MvInlineBlock -> Indent.getNormalIndent()
            else -> Indent.getNoneIndent()
        }
//        parentPsi is MvSpecExpr -> Indent.getNormalIndent()

        // binary expressions, chain calls
        // no indent on it's own, use parent indent
        parentPsi is MvExpr -> Indent.getIndent(Indent.Type.NONE, true, true)

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
