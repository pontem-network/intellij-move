package org.move.lang.core.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.move.lang.core.psi.MvNamedAddress
import org.move.lang.moveProject
import org.move.utils.doRenameIdentifier
import org.toml.lang.psi.TomlKeySegment

class NamedAddressReference(element: MvNamedAddress) : PsiReferenceBase<MvNamedAddress>(element) {
    override fun equals(other: Any?): Boolean =
        other is NamedAddressReference && element === other.element

    override fun hashCode(): Int = element.hashCode()

    override fun calculateDefaultRangeInElement(): TextRange {
        val anchor = element.referenceNameElement ?: return TextRange.EMPTY_RANGE
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

    override fun resolve(): TomlKeySegment? {
        val addressName = element.referenceName
        val addressVal = element.moveProject?.addresses()?.get(addressName) ?: return null
        return addressVal.tomlKeySegment
    }
}
