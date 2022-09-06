package org.move.ide.folding

import com.intellij.codeInsight.folding.CodeFoldingSettings
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.move.cli.settings.collapseSpecs
import org.move.lang.MoveFile
import org.move.lang.MoveParserDefinition.Companion.BLOCK_COMMENT
import org.move.lang.MoveParserDefinition.Companion.EOL_DOC_COMMENT
import org.move.lang.MvElementTypes.*
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.getNextNonCommentSibling
import org.move.lang.core.psi.ext.getNextNonWhitespaceSibling
import org.move.lang.core.psi.ext.startOffset

class MvFoldingBuilder : CustomFoldingBuilder(), DumbAware {
    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String {
        return when {
            node.elementType == USE_STMT -> "/* uses */"
            node.elementType == CONST -> "/* consts */"
            node.psi is MvFunctionParameterList -> "(...)"
            node.psi is PsiComment -> "/* ... */"
            node.psi is MvAcquiresType -> "/* acquires */"
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
        return node.psi.project.collapseSpecs && node.elementType == MODULE_SPEC_BLOCK
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

        override fun visitItemSpecBlock(o: MvItemSpecBlock) {
            if (o.children.isNotEmpty()) {
                fold(o)
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

        override fun visitUseStmt(o: MvUseStmt) = foldRepeatingItems(o, usesRanges)
        override fun visitConst(o: MvConst) = foldRepeatingItems(o, constRanges)

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

        private inline fun <reified T> foldRepeatingItems(startNode: T, ranges: MutableList<TextRange>) {
            if (isInRangesAlready(ranges, startNode as PsiElement)) return

            var lastNode: PsiElement? = null
            var tmpNode: PsiElement? = startNode

            while (tmpNode is T || tmpNode is PsiWhiteSpace) {
                tmpNode = tmpNode.getNextNonCommentSibling()
                if (tmpNode is T)
                    lastNode = tmpNode
            }

            if (lastNode == startNode) return

            if (lastNode != null) {
                val range = TextRange(startNode.startOffset, lastNode.endOffset)
                descriptors += FoldingDescriptor(startNode.node, range)
                ranges.add(range)
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
