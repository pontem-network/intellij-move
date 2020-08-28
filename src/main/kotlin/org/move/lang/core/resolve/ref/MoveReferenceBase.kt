package org.move.lang.core.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.resolve.ResolveEngine

abstract class MoveReferenceBase<T : MoveReferenceElement>(element: T) : PsiReferenceBase<T>(element),
                                                                         MoveReference {
    abstract fun resolveVerbose(): ResolveEngine.ResolveResult

    final override fun resolve(): MoveNamedElement? =
        resolveVerbose().let {
            when (it) {
                is ResolveEngine.ResolveResult.Resolved -> it.element
                else -> null
            }
        }

    open val T.referenceAnchor: PsiElement get() = referenceNameElement

    // enforce not nullability
    final override fun getRangeInElement(): TextRange = super.getRangeInElement()

    final override fun calculateDefaultRangeInElement(): TextRange {
        val anchor = element.referenceAnchor
        check(anchor.parent === element)
        return TextRange.from(anchor.startOffsetInParent, anchor.textLength)
    }
}