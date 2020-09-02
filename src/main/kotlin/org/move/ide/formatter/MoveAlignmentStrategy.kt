/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.formatter

import com.intellij.formatting.Alignment
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

interface MoveAlignmentStrategy {
    /**
     * Requests current strategy for alignment to use for given child.
     */
    fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MoveFmtContext): Alignment?

    /**
     * Always returns `null`.
     */
    object NullStrategy : MoveAlignmentStrategy {
        override fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MoveFmtContext): Alignment? = null
    }

    /**
     * Apply this strategy only when child element is in [tt].
     */
    fun alignIf(vararg tt: IElementType): MoveAlignmentStrategy = alignIf(TokenSet.create(*tt))

    /**
     * Apply this strategy only when child element type matches [filterSet].
     */
    fun alignIf(filterSet: TokenSet): MoveAlignmentStrategy =
        object : MoveAlignmentStrategy {
            override fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MoveFmtContext): Alignment? =
                if (child.elementType in filterSet) {
                    this@MoveAlignmentStrategy.getAlignment(child, parent, childCtx)
                } else {
                    null
                }
        }

    /**
     * Apply this strategy only when [predicate] passes.
     */
    fun alignIf(predicate: (child: ASTNode, parent: ASTNode?, ctx: MoveFmtContext) -> Boolean): MoveAlignmentStrategy =
        object : MoveAlignmentStrategy {
            override fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MoveFmtContext): Alignment? =
                if (predicate(child, parent, childCtx)) {
                    this@MoveAlignmentStrategy.getAlignment(child, parent, childCtx)
                } else {
                    null
                }
        }

    /**
     * Returns [NullStrategy] if [condition] is `false`. Useful for making strategies configurable.
     */
    fun alignIf(condition: Boolean): MoveAlignmentStrategy =
        if (condition) {
            this
        } else {
            NullStrategy
        }

    companion object {
        /**
         * Always returns [alignment].
         */
        fun wrap(alignment: Alignment = Alignment.createAlignment()): MoveAlignmentStrategy =
            object : MoveAlignmentStrategy {
                override fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MoveFmtContext): Alignment? =
                    alignment
            }

        /**
         * Always returns [MoveFmtContext.sharedAlignment]
         */
        fun shared(): MoveAlignmentStrategy =
            object : MoveAlignmentStrategy {
                override fun getAlignment(child: ASTNode, parent: ASTNode?, childCtx: MoveFmtContext): Alignment? =
                    childCtx.sharedAlignment
            }
    }
}
