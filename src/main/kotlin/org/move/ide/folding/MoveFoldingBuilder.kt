package org.move.ide.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveFunctionParameterList

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
        quick: Boolean
    ): Array<FoldingDescriptor> {
        return emptyArray()
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return false
    }

    private class FoldingVisitor
}
