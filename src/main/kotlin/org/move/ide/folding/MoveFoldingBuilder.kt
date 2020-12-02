package org.move.ide.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*

class MoveFoldingBuilder : FoldingBuilderEx(),
                           DumbAware {
    override fun getPlaceholderText(node: ASTNode): String =
        when (node.psi) {
            is MoveFunctionParameterList -> "(...)"
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
        return false
    }

    private class FoldingVisitor(private val descriptors: MutableList<FoldingDescriptor>) : MoveVisitor() {
        override fun visitCodeBlock(o: MoveCodeBlock) = fold(o)
        override fun visitScriptBlock(o: MoveScriptBlock) = fold(o)
        override fun visitModuleBlock(o: MoveModuleBlock) = fold(o)

        override fun visitFunctionParameterList(o: MoveFunctionParameterList) {
            if (o.functionParameterList.isNotEmpty())
                fold(o)
        }
        override fun visitStructFieldsDefBlock(o: MoveStructFieldsDefBlock) {
            if (o.structFieldDefList.isNotEmpty())
                fold(o)
        }

        private fun fold(element: PsiElement) {
            descriptors += FoldingDescriptor(element.node, element.textRange)
        }
    }
}
