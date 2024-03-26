package org.move.ide.folding

import com.intellij.codeInsight.folding.CodeFoldingSettings
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.nextLeaf
import org.move.cli.settings.moveSettings
import org.move.lang.MoveFile
import org.move.lang.MoveParserDefinition.Companion.BLOCK_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_DOC_COMMENT
import org.move.lang.MvElementTypes.*
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

class MvFoldingBuilder : CustomFoldingBuilder(), DumbAware {
    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String {
        when (node.elementType) {
            L_BRACE -> return " { "
            R_BRACE -> return " }"
            CONST -> return "..."
            USE_STMT -> return "..."
        }
        return when (node.psi) {
            is MvAcquiresType -> "/* acquires */"
            is PsiComment -> "/* ... */"
            is MvFunctionParameterList -> "(...)"
            else -> "{...}"
        }
    }

    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
        if (root !is MoveFile) return

        val usesRanges: MutableList<TextRange> = ArrayList()
        val constRanges: MutableList<TextRange> = ArrayList()
        val docCommentRanges: MutableList<TextRange> = ArrayList()

        val visitor = FoldingVisitor(descriptors, usesRanges, constRanges, docCommentRanges)
        PsiTreeUtil.processElements(root) { it.accept(visitor); true }
    }

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
        return node.psi.project.moveSettings.foldSpecs && node.elementType == MODULE_SPEC_BLOCK
                || CodeFoldingSettings.getInstance().isDefaultCollapsedNode(node)
    }

    private class FoldingVisitor(
        private val descriptors: MutableList<FoldingDescriptor>,
        private val usesRanges: MutableList<TextRange>,
        private val constRanges: MutableList<TextRange>,
        private val docCommentRanges: MutableList<TextRange>,
    ) : MvVisitor() {

        override fun visitCodeBlock(o: MvCodeBlock) = fold(o)
        override fun visitScriptBlock(o: MvScriptBlock) = fold(o)
        override fun visitModuleBlock(o: MvModuleBlock) = fold(o)

        override fun visitSpecCodeBlock(block: MvSpecCodeBlock) {
            if (block.children.isNotEmpty()) {
                fold(block)
            }
        }

        override fun visitAcquiresType(o: MvAcquiresType) = fold(o)

        override fun visitComment(comment: PsiComment) {
            when (comment.tokenType) {
                BLOCK_COMMENT -> fold(comment)
                EOL_DOC_COMMENT -> foldRepeatingDocComments(comment)
            }
        }

        override fun visitFunctionParameterList(o: MvFunctionParameterList) {
            if (o.functionParameterList.isNotEmpty())
                fold(o)
        }

        override fun visitStructBlock(o: MvStructBlock) {
            if (o.structFieldList.isNotEmpty())
                fold(o)
        }

        override fun visitUseStmt(o: MvUseStmt) = foldRepeatingItems(o, o.use, o.use, usesRanges)
        override fun visitConst(o: MvConst) = foldRepeatingItems(o, o.constKw, o.constKw, constRanges)

        private fun fold(element: PsiElement) {
            descriptors += FoldingDescriptor(element.node, element.textRange)
        }

        private fun foldBetween(element: PsiElement, left: PsiElement?, right: PsiElement?) {
            if (left != null && right != null && right.textLength > 0) {
                val range = TextRange(left.textOffset, right.textOffset + 1)
                descriptors += FoldingDescriptor(element.node, range)
            }
        }

        private fun foldRepeatingDocComments(startNode: PsiComment) {
            if (isInRangesAlready(docCommentRanges, startNode as PsiElement)) return

            var lastNode: PsiElement? = null
            var tmpNode: PsiElement? = startNode

            while (tmpNode.elementType == EOL_DOC_COMMENT || tmpNode is PsiWhiteSpace) {
                tmpNode = tmpNode.getNextNonWhitespaceSibling()
                if (tmpNode.elementType == EOL_DOC_COMMENT)
                    lastNode = tmpNode
            }

            if (lastNode == startNode) return

            if (lastNode != null) {
                val range = TextRange(startNode.startOffset, lastNode.endOffset)
                descriptors += FoldingDescriptor(startNode.node, range)
                docCommentRanges.add(range)
            }
        }

        private inline fun <reified T: MvDocAndAttributeOwner> foldRepeatingItems(
            startNode: T,
            startKeyword: PsiElement,
            endKeyword: PsiElement,
            ranges: MutableList<TextRange>
        ) {
            if (isInRangesAlready(ranges, startNode as PsiElement)) return

            val lastNode = startNode.rightSiblings
                .filterNot { it is PsiComment || it is PsiWhiteSpace }
                .takeWhile { it is T }
                .lastOrNull() ?: return

            val trailingSemicolon = when (lastNode) {
                is MvUseStmt -> lastNode.semicolon
                is MvConst -> lastNode.semicolon
                else -> null
            }

            val foldStartOffset = endKeyword.foldRegionStart()
            val foldEndOffset = trailingSemicolon?.startOffset ?: lastNode.endOffset

            // This condition may be false when only the leading keyword is present, but the node is malformed.
            // We don't collapse such nodes (even though they may have attributes).
            if (foldStartOffset < foldEndOffset) {
                val totalRange = TextRange(startNode.startOffset, lastNode.endOffset)
                ranges.add(totalRange)

                val group = FoldingGroup.newGroup("${T::class}")
                val primaryFoldRange = TextRange(foldStartOffset, foldEndOffset)
                // TODO: the leading keyword is excluded from folding. This makes it pretty on display, but
                //       disables the fold action on it (and also on the last semicolon, if present). I don't
                //       know whether it is possible to display those tokens unfolded, but still provide fold
                //       action. Kotlin, which implements similar import folding logic, doesn't solve that
                //       problem.
                descriptors += FoldingDescriptor(startNode.node, primaryFoldRange, group)

                if (startNode.startOffset < startKeyword.startOffset) {
                    // Hide leading attributes and doc comments
                    val attrRange = TextRange(startNode.startOffset, startKeyword.startOffset)
                    descriptors += FoldingDescriptor(startNode.node, attrRange, group, "")
                }
            }
        }

        private fun isInRangesAlready(ranges: MutableList<TextRange>, element: PsiElement?): Boolean {
            if (element == null) return false
            return ranges.any { x -> element.textOffset in x }
        }
    }
}

private fun CodeFoldingSettings.isDefaultCollapsedNode(node: ASTNode) =
    this.COLLAPSE_IMPORTS && node.elementType == USE_STMT
            || this.COLLAPSE_DOC_COMMENTS && node.elementType == EOL_DOC_COMMENT
//            || this.COLLAPSE_CUSTOM_FOLDING_REGIONS && node.elementType == ACQUIRES_TYPE

private fun PsiElement.foldRegionStart(): Int {
    val nextLeaf = nextLeaf(skipEmptyElements = true) ?: return endOffset
    return if (nextLeaf.text.startsWith(' ')) {
        endOffset + 1
    } else {
        endOffset
    }
}
