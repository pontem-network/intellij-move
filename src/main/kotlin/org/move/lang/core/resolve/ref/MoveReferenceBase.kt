package org.move.lang.core.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.move.lang.core.psi.MvReferenceElement
import org.move.utils.doRenameIdentifier

abstract class MvReferenceBase<T : MvReferenceElement>(element: T) : PsiReferenceBase<T>(element),
                                                                         MvReference {

    open val T.referenceAnchor: PsiElement? get() = referenceNameElement

    override fun equals(other: Any?): Boolean = other is MvReferenceBase<*> && element === other.element

    override fun hashCode(): Int = element.hashCode()

    final override fun getRangeInElement(): TextRange = super.getRangeInElement()

    final override fun calculateDefaultRangeInElement(): TextRange {
        val anchor = element.referenceAnchor ?: return TextRange.EMPTY_RANGE
        return TextRange.from(
            anchor.startOffsetInParent,
            anchor.textLength
        )
    }

    override fun handleElementRename(newElementName: String): PsiElement? {
        val refNameElement = element.referenceNameElement ?: return null
        doRenameIdentifier(refNameElement, newElementName)
        return element
    }
}
