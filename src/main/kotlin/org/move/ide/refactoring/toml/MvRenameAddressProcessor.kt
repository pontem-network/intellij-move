package org.move.ide.refactoring.toml

import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.move.openapiext.addressesTable
import org.toml.lang.psi.TomlKeySegment

class MvRenameAddressProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is TomlKeySegment && element.addressesTable != null
    }
}
