package org.move.ide.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.formatter.common.AbstractBlock
import org.move.ide.formatter.impl.*
import org.move.lang.MvElementTypes.*

private fun ASTNode.isLeafNode(): Boolean = this.firstChildNode == null

// every node in the formatter is wrapped in this block, formatting applied to blocks
class MoveFmtBlock(
    val blockNode: ASTNode,
    val blockWrap: Wrap?,
    val blockAlignment: Alignment?,
    val blockIndent: Indent?,
    val blockCtx: MvFmtContext,
    val blockName: String,
) : AbstractBlock(blockNode, blockWrap, blockAlignment) {

    override fun isLeaf(): Boolean = node.isLeafNode()

    override fun getIndent(): Indent? = blockIndent

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = computeSpacing(child1, child2, blockCtx)

    private val mySubBlocks: List<Block> by lazy { buildChildren() }
    override fun getSubBlocks(): List<Block> = mySubBlocks

    override fun getDebugName() = blockName

    override fun buildChildren(): List<Block> {
        val parentType = node.elementType
        val childAlignment = when (parentType) {
            // new alignment
            FUNCTION_PARAMETER_LIST -> Alignment.createAlignment()
            // aligned with parent
            FUNCTION_PARAMETER -> blockCtx.sharedAlignment
            else -> null
        }
        val alignment = getAlignmentStrategy()

        val noneWrap = Wrap.createWrap(WrapType.NONE, false)
        val chopListWrap = Wrap.createWrap(WrapType.CHOP_DOWN_IF_LONG, true)

        return node.getChildren(null)
            .filter { !it.isWhitespaceOrEmpty() }
            .map { childNode ->
                val childType = childNode.elementType
                val childCtx = blockCtx.copy(sharedAlignment = childAlignment)
                val indent = computeChildIndent(childNode)
                val isLeaf = childNode.isLeafNode()

                val childWrap = when {
                    isLeaf && childType !in setOf(R_PAREN, R_BRACE) -> noneWrap
                    parentType == FUNCTION_PARAMETER_LIST -> chopListWrap
                    parentType == VALUE_ARGUMENT_LIST -> chopListWrap
                    parentType == ATTR_ITEM_ARGUMENTS -> chopListWrap
                    parentType == USE_ITEM_GROUP -> chopListWrap
                    else -> null
                }

                MoveFmtBlock(
                    blockNode = childNode,
                    blockWrap = childWrap,
                    blockAlignment = alignment.getAlignment(childNode, node, childCtx),
                    blockIndent = indent,
                    blockCtx = childCtx,
                    blockName = "${if (isLeaf) "LeafBlock" else "NodeBlock"}[$childType]"
                )
            }
    }

    // automatic indentation on typing, see sdk docs
    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val indent = when (node.elementType) {
            ADDRESS_BLOCK -> Indent.getNoneIndent()

            // We are inside some kind of {...}, [...], (...) or <...> block
            //            node.isDelimitedBlock -> Indent.getNormalIndent()
            in DELIMITED_BLOCKS -> Indent.getNormalIndent()

            // Otherwise we don't want any indentation (null means continuation indent)
            else -> Indent.getNoneIndent()
        }
        return ChildAttributes(indent, null)
    }
}
