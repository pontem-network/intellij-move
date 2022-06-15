package org.move.ide.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameHandler
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.openapiext.addressesTable
import org.toml.lang.psi.TomlKeySegment

class MvRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean =
        element is MvNameIdentifierOwner
}

class TomlRefactoringSupportProvider: RefactoringSupportProvider() {
    override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return element is TomlKeySegment && element.addressesTable != null
    }
}
