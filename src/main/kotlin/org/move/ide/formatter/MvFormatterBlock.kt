package org.move.ide.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.formatter.common.AbstractBlock
import org.move.ide.formatter.impl.*
import org.move.lang.MvElementTypes.FUNCTION_PARAM
import org.move.lang.MvElementTypes.FUNCTION_PARAMS

class MvFormatterBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val indent: Indent?,
    val ctx: MvFmtContext
) : AbstractBlock(node, wrap, alignment) {
    override fun isLeaf(): Boolean = node.firstChildNode == null
    override fun getIndent(): Indent? = indent

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = computeSpacing(child1, child2, ctx)

    override fun getSubBlocks(): List<Block> = mySubBlocks
    private val mySubBlocks: List<Block> by lazy { buildChildren() }

    override fun buildChildren(): List<Block> {
        val sharedAlignment = when (node.elementType) {
            FUNCTION_PARAMS -> Alignment.createAlignment()
            FUNCTION_PARAM -> ctx.sharedAlignment
            else -> null
        }
        val alignment = getAlignmentStrategy()

        return node.getChildren(null)
            .filter { !it.isWhitespaceOrEmpty() }
            .map { childNode: ASTNode ->
                val childCtx = ctx.copy(sharedAlignment = sharedAlignment)
                MvFormatterBlock(
                    node = childNode,
                    alignment = alignment.getAlignment(childNode, node, childCtx),
                    indent = computeIndent(childNode),
                    wrap = null,
                    ctx = childCtx
                )
            }
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val indent = when {
            node.isDelimitedBlock -> Indent.getNormalIndent()
            // Otherwise we don't want any indentation (null means continuation indent)
            else -> Indent.getNoneIndent()
        }
        return ChildAttributes(indent, null)
    }

    private fun computeIndent(child: ASTNode): Indent? {
//        val parentType = node.elementType
//        val parentPsi = node.psi
//        val childType = child.elementType
//        val childPsi = child.psi
        return when {
            node.isDelimitedBlock -> getIndentIfNotDelim(child, node)
            else -> Indent.getNoneIndent()
        }
    }
}