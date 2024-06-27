package org.move.ide.formatter.impl

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import org.move.ide.formatter.MoveFmtBlock
import org.move.lang.MvElementTypes.*
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvPragmaSpecStmt

fun MoveFmtBlock.computeChildIndent(childNode: ASTNode): Indent? {
    val parentNode = node
    val parentPsi = node.psi
    val parentType = node.elementType
    return when {
        // do not indent contents of an address block
        // address {
        // module M {
        // }
        // }
        parentType == ADDRESS_BLOCK -> Indent.getNoneIndent()

        // indent inline block in else block
        // if (true)
        // else
        //     2 + 2;
        parentType == ELSE_BLOCK
                && childNode.elementType == INLINE_BLOCK -> Indent.getContinuationIndent()

        // do not indent else block
        childNode.elementType == ELSE_BLOCK -> Indent.getNoneIndent()

        // indent every child of the block except for braces
        // module M {
        //    struct S {}
        // }
        parentType in DELIMITED_BLOCKS -> getNormalIndentIfNotCurrentBlockDelimiter(childNode, parentNode)

//        //     let a =
//        //     92;
//        // =>
//        //     let a =
//        //         92;
        parentType == INITIALIZER -> Indent.getNormalIndent()

        // in expressions, we need to indent any part of it except for the first one
        // - binary expressions
        // 10000
        //     + 2
        //     - 3
        // - field chain calls
        // get_s()
        //     .myfield
        //     .myotherfield
        parentPsi is MvExpr -> Indent.getContinuationWithoutFirstIndent()

        // same thing as previous one, but for spec statements
        parentPsi.isSpecStmt -> Indent.getContinuationWithoutFirstIndent()

        else -> Indent.getNoneIndent()
    }
}

fun getNormalIndentIfNotCurrentBlockDelimiter(child: ASTNode, parent: ASTNode): Indent =
    if (child.isDelimiterOfCurrentBlock(parent)) {
        Indent.getNoneIndent()
    } else {
        Indent.getNormalIndent()
    }
