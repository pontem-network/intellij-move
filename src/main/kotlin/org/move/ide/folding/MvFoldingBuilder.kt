package org.move.ide.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.MvElementTypes.SPEC_BLOCK
import org.move.lang.core.psi.*
import org.move.settings.collapseSpecs

class MvFoldingBuilder : FoldingBuilderEx(),
                         DumbAware {
    override fun getPlaceholderText(node: ASTNode): String =
        when (node.psi) {
            is MvFunctionParameterList -> "(...)"
            else -> "{...}"
        }

    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean,
    ): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val visitor = FoldingVisitor(descriptors)
        PsiTreeUtil.processElements(root) { it.accept(visitor); true }
        return descriptors.toTypedArray()
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return node.psi.project.collapseSpecs
                && node.elementType == SPEC_BLOCK
    }

    private class FoldingVisitor(private val descriptors: MutableList<FoldingDescriptor>) : MvVisitor() {
        override fun visitCodeBlock(o: MvCodeBlock) = fold(o)
        override fun visitScriptBlock(o: MvScriptBlock) = fold(o)
        override fun visitModuleBlock(o: MvModuleBlock) = fold(o)

        override fun visitSpecBlock(o: MvSpecBlock) {
            if (o.children.isNotEmpty()) {
                fold(o)
            }
        }

        override fun visitFunctionParameterList(o: MvFunctionParameterList) {
            if (o.functionParameterList.isNotEmpty())
                fold(o)
        }

        override fun visitStructFieldsDefBlock(o: MvStructFieldsDefBlock) {
            if (o.structFieldDefList.isNotEmpty())
                fold(o)
        }

        private fun fold(element: PsiElement) {
            descriptors += FoldingDescriptor(element.node, element.textRange)
        }
    }
}
