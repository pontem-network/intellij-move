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
import org.move.lang.MvElementTypes.*
import org.move.lang.core.MOVE_COMMENTS
import org.move.lang.core.MOVE_KEYWORDS
import org.move.lang.core.psi.MvAddressBlock
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.getNextNonCommentSibling
import org.move.lang.core.psi.ext.getPrevNonCommentSibling
import org.move.lang.core.tokenSetOf

fun createSpacingBuilder(commonSettings: CommonCodeStyleSettings): SpacingBuilder {
    return SpacingBuilder(commonSettings)
        .afterInside(COMMA, BLOCK_FIELDS).parentDependentLFSpacing(1, 1, true, 1)
        .after(COMMA).spacing(1, 1, 0, true, 0)
        .before(COMMA).spaceIf(false)
        .after(COLON).spaceIf(true)
        .before(COLON).spaceIf(false)

        .before(TYPE_ANNOTATION).spacing(0, 0, 0, true, 0)
        .before(INITIALIZER).spacing(1, 1, 0, true, 0)

        .between(AND, MUT).spacing(0, 0, 0, false, 0)

        .afterInside(AND, PATH_TYPE).spacing(0, 0, 0, false, 0)
        .afterInside(AND, BORROW_EXPR).spacing(0, 0, 0, false, 0)
        .afterInside(MUL, DEREF_EXPR).spacing(0, 0, 0, false, 0)

        //== empty parens
        .between(L_BRACE, R_BRACE).spacing(0, 0, 0, false, 0)
        .between(L_PAREN, R_PAREN).spacing(0, 0, 0, false, 0)

        //== paren delimited lists
        .afterInside(L_PAREN, PAREN_DELIMITED_BLOCKS).spacing(0, 0, 0, true, 0)
        .beforeInside(R_PAREN, PAREN_DELIMITED_BLOCKS).spacing(0, 0, 0, true, 0)
        .afterInside(LT, ANGLE_DELIMITED_BLOCKS).spacing(0, 0, 0, true, 0)
        .beforeInside(GT, ANGLE_DELIMITED_BLOCKS).spacing(0, 0, 0, true, 0)

        .afterInside(L_BRACE, BLOCK_LIKE).parentDependentLFSpacing(1, 1, true, 0)
        .beforeInside(R_BRACE, BLOCK_LIKE).parentDependentLFSpacing(1, 1, true, 0)
//        .withinPairInside(L_BRACE, R_BRACE, STRUCT_PAT).spacing(1, 1, 0, true, 0)
//        .withinPairInside(L_BRACE, R_BRACE, STRUCT_LIT_EXPR).spacing(1, 1, 0, true, 0)

        // handling `module 0x1::M {}` and `let S { f1, f2: f }` blocks
        .beforeInside(L_BRACE, FLAT_BRACE_BLOCKS).spaces(1)
        .afterInside(L_BRACE, FLAT_BRACE_BLOCKS)
        .parentDependentLFSpacing(1, 1, true, 0)
        .beforeInside(R_BRACE, FLAT_BRACE_BLOCKS)
        .parentDependentLFSpacing(1, 1, true, 0)
        .withinPairInside(L_BRACE, R_BRACE, STRUCT_PAT)
        .spacing(1, 1, 0, true, 0)

//        .afterInside(L_BRACE, tokenSetOf(STRUCT_PAT_FIELDS_BLOCK, STRUCT_LIT_FIELDS_BLOCK)).spaces(1)
//        .beforeInside(R_BRACE, tokenSetOf(STRUCT_PAT_FIELDS_BLOCK, STRUCT_LIT_FIELDS_BLOCK)).spaces(1)
//        .beforeInside(L_BRACE, tokenSetOf(STRUCT_PAT, STRUCT_LIT_EXPR)).spaces(1)

        //== items
        .between(FUNCTION_PARAMETER_LIST, RETURN_TYPE).spaceIf(false)

        .between(IDENTIFIER, FUNCTION_PARAMETER_LIST).spaceIf(false)
        .between(IDENTIFIER, VALUE_ARGUMENT_LIST).spaceIf(false)
        .between(IDENTIFIER, TYPE_PARAMETER_LIST).spaceIf(false)
        .between(IDENTIFIER, TYPE_ARGUMENT_LIST).spaceIf(false)

//        .between(IDENTIFIER, STRUCT_LIT_FIELDS_BLOCK).spaceIf(true)
//        .between(IDENTIFIER, STRUCT_LIT_FIELDS_BLOCK).spaceIf(true)

        .between(TYPE_PARAMETER_LIST, FUNCTION_PARAMETER_LIST).spaceIf(false)
        .before(VALUE_ARGUMENT_LIST).spaceIf(false)

        .betweenInside(PUBLIC, L_PAREN, VISIBILITY_MODIFIER).spaces(0)
        .betweenInside(
            tokenSetOf(L_PAREN),
            tokenSetOf(SCRIPT_KW, FRIEND),
            VISIBILITY_MODIFIER
        ).spaces(0)
        .betweenInside(
            tokenSetOf(SCRIPT_KW, FRIEND),
            tokenSetOf(R_PAREN),
            VISIBILITY_MODIFIER
        ).spaces(0)
        .after(VISIBILITY_MODIFIER).spaces(1)

        .around(BINARY_OP).spaces(1)
        .around(EQ).spaces(1)
        .around(MOVE_KEYWORDS).spaces(1)

        .applyForEach(BLOCK_LIKE) { before(it).spaces(1) }

        .before(ACQUIRES_TYPE).spaces(1)
//        .applyForEach(STRUCT_LITERAL_BLOCKS) { before(it).spaces(0) }
}


fun Block.computeSpacing(child1: Block?, child2: Block, ctx: MvFmtContext): Spacing? {
    if (child1 is ASTBlock && child2 is ASTBlock) SpacingContext.create(child1, child2)?.apply {
        when {
            ncPsi1.isStmt && ncPsi2.isStmtOrExpr
            -> return lineBreak(
                keepLineBreaks = ctx.commonSettings.KEEP_LINE_BREAKS,
                keepBlankLines = ctx.commonSettings.KEEP_BLANK_LINES_IN_CODE
            )
            ncPsi1.isTopLevelItem && ncPsi2.isTopLevelItem
            -> return lineBreak(
                minLineFeeds = if (!needsBlankLineBetweenItems()) 0 else 1,
                keepLineBreaks = ctx.commonSettings.KEEP_LINE_BREAKS,
                keepBlankLines = ctx.commonSettings.KEEP_BLANK_LINES_IN_DECLARATIONS
            )

            ncPsi1.isDeclarationItem && ncPsi2.isDeclarationItem
            -> return lineBreak(
                minLineFeeds = 1 + if (!needsBlankLineBetweenItems()) 0 else 1,
                keepLineBreaks = ctx.commonSettings.KEEP_LINE_BREAKS,
                keepBlankLines = ctx.commonSettings.KEEP_BLANK_LINES_IN_DECLARATIONS
            )

            (ncPsi1.text == "{" && ncPsi1.parent is MvAddressBlock && ncPsi2 is MvModule)
                    || (ncPsi1.text == "{" && ncPsi1.parent is MvModule && ncPsi2.isModuleItem)
            -> return lineBreak(
                minLineFeeds = if (!needsBlankLineBetweenItems()) 0 else 1,
                keepLineBreaks = ctx.commonSettings.KEEP_LINE_BREAKS,
                // TODO: make formatter setting MAX_BLANK_LINES_AFTER_ADDRESS_DEF
                keepBlankLines = 1
            )

        }
    }
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
    val ncPsi2: PsiElement,
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
            node2: ASTNode, psi2: PsiElement,
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
    tokenSet: TokenSet, block: SpacingBuilder.(IElementType) -> SpacingBuilder,
): SpacingBuilder {
    var self = this
    for (tt in tokenSet.types) {
        self = block(this, tt)
    }
    return self
}

private fun lineBreak(
    minLineFeeds: Int = 1,
    keepLineBreaks: Boolean = true,
    keepBlankLines: Int = 1,
): Spacing =
    Spacing.createSpacing(0, Int.MAX_VALUE, minLineFeeds, keepLineBreaks, keepBlankLines)

private fun ASTNode.hasLineBreakAfterInSameParent(): Boolean =
    treeNext != null && TreeUtil.findFirstLeaf(treeNext).isWhiteSpaceWithLineBreak()

private fun ASTNode.hasLineBreakBeforeInSameParent(): Boolean =
    treePrev != null && TreeUtil.findLastLeaf(treePrev).isWhiteSpaceWithLineBreak()

private fun ASTNode?.isWhiteSpaceWithLineBreak(): Boolean =
    this != null && elementType == TokenType.WHITE_SPACE && textContains('\n')

private fun SpacingContext.needsBlankLineBetweenItems(): Boolean {
    if (elementType1 in MOVE_COMMENTS || elementType2 in MOVE_COMMENTS)
        return false

    // Allow to keep consecutive runs of `use`, `const` or other "one line" items without blank lines
    return elementType1 !in ONE_LINE_ITEMS || elementType1 != elementType2
}

fun SpacingBuilder.beforeInside(
    element: IElementType,
    tokenSet: TokenSet,
    spacingFun: SpacingBuilder.RuleBuilder.() -> Unit
) {
    tokenSet.types.forEach { inType -> beforeInside(element, inType).spacingFun() }
}

fun SpacingBuilder.afterInside(
    element: IElementType,
    tokenSet: TokenSet,
    spacingFun: SpacingBuilder.RuleBuilder.() -> Unit
) {
    tokenSet.types.forEach { inType -> afterInside(element, inType).spacingFun() }
}

fun SpacingBuilder.RuleBuilder.spacesNoLineBreak(spaces: Int): SpacingBuilder? =
    spacing(spaces, spaces, 0, false, 0)
