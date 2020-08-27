package org.move.ide.formatter.impl

import com.intellij.formatting.ASTBlock
import com.intellij.formatting.Block
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.move.ide.formatter.MvFmtContext
import org.move.lang.core.MV_KEYWORDS
import org.move.lang.core.psi.ext.getNextNonCommentSibling
import org.move.lang.core.psi.ext.getPrevNonCommentSibling

fun createSpacingBuilder(commonSettings: CommonCodeStyleSettings): SpacingBuilder {
    return SpacingBuilder(commonSettings)
        .around(BINARY_OPS).spaces(1)
        .around(MV_KEYWORDS).spaces(1)
        .applyForEach(BLOCK_LIKE) { before(it).spaces(1) }
}

fun Block.computeSpacing(child1: Block?, child2: Block, ctx: MvFmtContext): Spacing? {
//    if (child1 is ASTBlock && child2 is ASTBlock) SpacingContext.create(child1, child2)
    return ctx.spacingBuilder.getSpacing(this, child1, child2)
}

private data class SpacingContext(
    val node1: ASTNode,
    val node2: ASTNode,
    val psi1: PsiElement,
    val psi2: PsiElement,
    val elementType1: IElementType,
    val elementType2: IElementType,
    val parentType: IElementType?,
    val parentPsi: PsiElement?,
    val ncPsi1: PsiElement,
    val ncPsi2: PsiElement
) {
    companion object {
        fun create(child1: ASTBlock, child2: ASTBlock): SpacingContext? {
            val node1 = child1.node ?: return null
            val node2 = child2.node ?: return null
            val psi1 = node1.psi
            val psi2 = node2.psi
            val elementType1 = psi1.node.elementType
            val elementType2 = psi2.node.elementType
            val parentType = node1.treeParent.elementType
            val parentPsi = psi1.parent
            val (ncPsi1, ncPsi2) = omitCommentBlocks(node1, psi1, node2, psi2)
            return SpacingContext(
                node1, node2, psi1, psi2, elementType1, elementType2,
                parentType, parentPsi, ncPsi1, ncPsi2
            )
        }

        /**
         * Handle blocks of comments to get proper spacing between items and statements
         */
        private fun omitCommentBlocks(
            node1: ASTNode, psi1: PsiElement,
            node2: ASTNode, psi2: PsiElement
        ): Pair<PsiElement, PsiElement> =
            Pair(
                if (psi1 is PsiComment && node1.hasLineBreakAfterInSameParent()) {
                    psi1.getPrevNonCommentSibling() ?: psi1
                } else {
                    psi1
                },
                if (psi2 is PsiComment && node2.hasLineBreakBeforeInSameParent()) {
                    psi2.getNextNonCommentSibling() ?: psi2
                } else {
                    psi2
                }
            )
    }
}

private inline fun SpacingBuilder.applyForEach(
    tokenSet: TokenSet, block: SpacingBuilder.(IElementType) -> SpacingBuilder
): SpacingBuilder {
    var self = this
    for (tt in tokenSet.types) {
        self = block(this, tt)
    }
    return self
}

private fun ASTNode.hasLineBreakAfterInSameParent(): Boolean =
    treeNext != null && TreeUtil.findFirstLeaf(treeNext).isWhiteSpaceWithLineBreak()

private fun ASTNode.hasLineBreakBeforeInSameParent(): Boolean =
    treePrev != null && TreeUtil.findLastLeaf(treePrev).isWhiteSpaceWithLineBreak()

private fun ASTNode?.isWhiteSpaceWithLineBreak(): Boolean =
    this != null && elementType == TokenType.WHITE_SPACE && textContains('\n')

