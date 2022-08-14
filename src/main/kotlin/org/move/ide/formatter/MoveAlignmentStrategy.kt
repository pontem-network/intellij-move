/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.formatter

import com.intellij.formatting.Alignment
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

interface MvAlignmentStrategy {
    /**
     * Requests current strategy for alignment to use for given child.
     */
    fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MvFmtContext): Alignment?

    /**
     * Always returns `null`.
     */
    object NullStrategy : MvAlignmentStrategy {
        override fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MvFmtContext): Alignment? = null
    }

    /**
     * Apply this strategy only when child element is in [tt].
     */
    fun alignIf(vararg tt: IElementType): MvAlignmentStrategy = alignIf(TokenSet.create(*tt))

    /**
     * Apply this strategy only when child element type matches [filterSet].
     */
    fun alignIf(filterSet: TokenSet): MvAlignmentStrategy =
        object : MvAlignmentStrategy {
            override fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MvFmtContext): Alignment? =
                if (child.elementType in filterSet) {
                    this@MvAlignmentStrategy.getAlignment(child, parent, childCtx)
                } else {
                    null
                }
        }

    /**
     * Apply this strategy only when [predicate] passes.
     */
    fun alignIf(predicate: (child: ASTNode, parent: ASTNode?, ctx: MvFmtContext) -> Boolean): MvAlignmentStrategy =
        object : MvAlignmentStrategy {
            override fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MvFmtContext): Alignment? =
                if (predicate(child, parent, childCtx)) {
                    this@MvAlignmentStrategy.getAlignment(child, parent, childCtx)
                } else {
                    null
                }
        }

    /**
     * Returns [NullStrategy] if [condition] is `false`. Useful for making strategies configurable.
     */
    fun alignIf(condition: Boolean): MvAlignmentStrategy =
        if (condition) {
            this
        } else {
            NullStrategy
        }

    companion object {
        /**
         * Always returns [alignment].
         */
        fun wrap(alignment: Alignment = Alignment.createAlignment()): MvAlignmentStrategy =
            object : MvAlignmentStrategy {
                override fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MvFmtContext): Alignment =
                    alignment
            }

        /**
         * Always returns [MvFmtContext.sharedAlignment]
         */
        fun shared(): MvAlignmentStrategy =
            object : MvAlignmentStrategy {
                override fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MvFmtContext): Alignment? =
                    childCtx.sharedAlignment
            }
    }
}
