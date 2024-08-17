package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.MvLetStmt
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.owner

class MvCompletionConfidence : CompletionConfidence() {
    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        // Don't show completion popup when typing a `let binding` identifier starting with a lowercase letter.
        // If the identifier is uppercase, the user probably wants to type a destructuring pattern
        // (`let Foo { ... }`), so we show the completion popup in this case
        if (contextElement.elementType == IDENTIFIER) {
            val parent = contextElement.parent
            if (parent is MvPatBinding && parent.owner is MvLetStmt) {
                val identText = contextElement.node.chars
                if (identText.firstOrNull()?.isLowerCase() == true) {
                    return ThreeState.YES
                }
            }
        }
        return ThreeState.UNSURE
    }
}
