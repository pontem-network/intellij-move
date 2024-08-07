package org.move.ide.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.formatter.common.AbstractBlock
import org.move.ide.formatter.blocks.SyntheticRsFmtBlock
import org.move.ide.formatter.impl.*
import org.move.lang.MvElementTypes.*

private fun ASTNode.isLeafNode(): Boolean = this.firstChildNode == null

// every node in the formatter is wrapped in this block, formatting applied to blocks
class MoveFmtBlock(
    private val node: ASTNode,
    private val wrap: Wrap?,
    private val alignment: Alignment?,
    private val indent: Indent?,
    val ctx: MvFmtContext,
    private val debugName: String,
) : AbstractBlock(node, wrap, alignment) {

    override fun isLeaf(): Boolean = node.isLeafNode()

    override fun getIndent(): Indent? = indent

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = computeSpacing(child1, child2, ctx)

    private val mySubBlocks: List<Block> by lazy { buildChildren() }
    override fun getSubBlocks(): List<Block> = mySubBlocks

    override fun buildChildren(): List<Block> {
        val parentType = node.elementType
        val childAlignment = when (parentType) {
            // new alignment
            FUNCTION_PARAMETER_LIST -> Alignment.createAlignment()
            // aligned with parent
            FUNCTION_PARAMETER -> ctx.sharedAlignment
            DOT_EXPR ->
                if (node.treeParent.elementType == DOT_EXPR)
                    ctx.sharedAlignment
                else
                    Alignment.createAlignment()
            else -> null
        }

        var metLBrace = false
        val alignment = getAlignmentStrategy()

        val noneWrap = Wrap.createWrap(WrapType.NONE, false)
        val chopListWrap = Wrap.createWrap(WrapType.CHOP_DOWN_IF_LONG, true)

        val children = node.getChildren(null)
            .filter { !it.isWhitespaceOrEmpty() }
            .map { childNode ->
                if (node.isFlatBlock && childNode.isBlockDelim(node)) {
                    metLBrace = true
                }

                val childType = childNode.elementType
                val childCtx = ctx.copy(
                    metLBrace = metLBrace,
                    sharedAlignment = childAlignment
                )
                val indent = computeIndent(childNode, childCtx)
                val isLeaf = childNode.isLeafNode()

                val childWrap = when {
                    isLeaf && childType !in setOf(R_PAREN, R_BRACE) -> noneWrap
                    parentType == FUNCTION_PARAMETER_LIST -> chopListWrap
                    parentType == VALUE_ARGUMENT_LIST -> chopListWrap
                    parentType == ATTR_ITEM_LIST -> chopListWrap
                    parentType == USE_GROUP -> chopListWrap
                    else -> null
                }

                MoveFmtBlock(
                    node = childNode,
                    wrap = childWrap,
                    alignment = alignment.getAlignment(childNode, node, childCtx),
                    indent = indent,
                    ctx = childCtx,
                    debugName = "${if (isLeaf) "LeafBlock" else "NodeBlock"}[$childType]"
                )
            }

        // Create fake `.sth` block here, so child indentation will
        // be relative to it when it starts from new line.
        // In other words: foo().bar().baz() => foo().baz()[.baz()]
        // We are using dot as our representative.
        // The idea is nearly copy-pasted from Kotlin's formatter.
        if (node.elementType == DOT_EXPR) {
            val dotIndex = children.indexOfFirst { it.node.elementType == DOT }
            if (dotIndex != -1) {
                val dotBlock = children[dotIndex]
                val syntheticBlock = SyntheticRsFmtBlock(
                    representative = dotBlock,
                    subBlocks = children.subList(dotIndex, children.size),
                    ctx = ctx)
                return children.subList(0, dotIndex).plusElement(syntheticBlock)
            }
        }
        return children
    }

    // automatic indentation on typing, see sdk docs
    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val indent = when {
            // Flat brace blocks do not have separate PSI node for content blocks
            // so we have to manually decide whether new child is before (no indent)
            // or after (normal indent) left brace node.
            node.isFlatBraceBlock -> {
                val lbraceIndex = subBlocks.indexOfFirst { it is ASTBlock && it.node?.elementType == L_BRACE }
                if (lbraceIndex != -1 && lbraceIndex < newChildIndex) {
                    Indent.getNormalIndent()
                } else {
                    Indent.getNoneIndent()
                }
            }

            node.elementType == ADDRESS_BLOCK -> Indent.getNoneIndent()

            // We are inside some kind of {...}, [...], (...) or <...> block
            //            node.isDelimitedBlock -> Indent.getNormalIndent()
            node.elementType in DELIMITED_BLOCKS -> Indent.getNormalIndent()

            // Otherwise we don't want any indentation (null means continuation indent)
            else -> Indent.getNoneIndent()
        }
        return ChildAttributes(indent, null)
    }

    override fun getDebugName() = debugName
    override fun toString() = "${node.text} $textRange"
}
