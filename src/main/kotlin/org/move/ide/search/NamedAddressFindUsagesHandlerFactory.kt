package org.move.ide.search

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.ext.isNamedAddressDef
import org.toml.lang.psi.TomlKeySegment

class NamedAddressFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    override fun canFindUsages(element: PsiElement): Boolean {
        return (element as? TomlKeySegment)?.isNamedAddressDef() ?: false
    }

    override fun createFindUsagesHandler(
        element: PsiElement,
        forHighlightUsages: Boolean
    ): FindUsagesHandler {
        return object : FindUsagesHandler(element) {}
    }
}
