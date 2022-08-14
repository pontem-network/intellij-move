package org.move.lang.core.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.MvReferenceElement
import org.move.utils.doRenameIdentifier

abstract class MvReferenceBase<T : MvReferenceElement>(element: T) : PsiPolyVariantReferenceBase<T>(element),
                                                                     MvReference {

    override fun resolve(): MvNamedElement? = super.resolve() as? MvNamedElement

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        multiResolve().map { PsiElementResolveResult(it) }.toTypedArray()

    override fun equals(other: Any?): Boolean =
        other is MvReferenceBase<*> && element === other.element

    override fun hashCode(): Int = element.hashCode()

    override fun getRangeInElement(): TextRange {
        return super.getRangeInElement()
    }

    final override fun calculateDefaultRangeInElement(): TextRange {
        val anchor = element.referenceNameElement
        if (anchor == null) {
            return TextRange.EMPTY_RANGE
        }
        val textRange = TextRange.from(
            anchor.startOffsetInParent,
            anchor.textLength
        )
        return textRange
    }

    override fun handleElementRename(newName: String): PsiElement {
        val refNameElement = element.referenceNameElement
        if (refNameElement != null) {
            doRenameIdentifier(refNameElement, newName)
        }
        return element
    }
}
