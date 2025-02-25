package org.move.lang.core.resolve

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.contexts
import org.move.lang.core.psi.ext.label.MvLabeledExpression
import org.move.lang.core.resolve.ref.Namespace

fun resolveLabelReference(element: MvLabel): List<MvNamedElement> {
    return collectResolveVariants(element.referenceName) {
        processLabelResolveVariants(element, it)
    }
}

fun processLabelResolveVariants(label: MvLabel, processor: RsResolveProcessor): Boolean {
    for (scope in label.contexts) {
        if (isLabelBarrier(scope)) return false
        if (scope is MvLabeledExpression) {
            val labelScopeEntry = scope.labelDecl?.asEntry() ?: continue
            if (processor.process(labelScopeEntry)) return true
        }
    }
    return false
}

private fun isLabelBarrier(scope: PsiElement): Boolean {
    return scope is MvLambdaExpr || scope is MvFunction || scope is MvConst
}
