package org.move.lang.core.resolve

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.contexts
import org.move.lang.core.psi.ext.label.MvLabeledExpression
import org.move.lang.core.resolve.ref.Namespace

fun getLabelResolveVariants(label: MvLabel): List<ScopeEntry> {
    return buildList {
        for (scope in label.contexts) {
            if (isLabelBarrier(scope)) return@buildList
            if (scope is MvLabeledExpression) {
                val labelScopeEntry = scope.labelDecl?.asEntry()
                if (labelScopeEntry != null) {
                    add(labelScopeEntry)
                }
            }
        }
    }
}

private fun isLabelBarrier(scope: PsiElement): Boolean {
    return scope is MvLambdaExpr || scope is MvFunction || scope is MvConst
}
