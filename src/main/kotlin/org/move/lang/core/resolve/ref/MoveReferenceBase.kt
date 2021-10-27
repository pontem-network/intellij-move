package org.move.lang.core.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.move.ide.refactoring.isValidMoveVariableIdentifier
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MovePsiFactory
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.psi.ext.elementType
import org.move.utils.doRenameIdentifier

abstract class MoveReferenceBase<T : MoveReferenceElement>(element: T) : PsiReferenceBase<T>(element),
                                                                         MoveReference {

    open val T.referenceAnchor: PsiElement? get() = referenceNameElement

    override fun equals(other: Any?): Boolean = other is MoveReferenceBase<*> && element === other.element

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
