package org.move.ide.search

import com.intellij.lang.HelpID
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveNamedElement

class MoveFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner() = MoveWordsScanner()

    override fun canFindUsagesFor(psiElement: PsiElement) =
        psiElement is MoveNamedElement

    override fun getHelpId(element: PsiElement) = HelpID.FIND_OTHER_USAGES

    override fun getType(element: PsiElement) = ""
    override fun getDescriptiveName(element: PsiElement) = ""
    override fun getNodeText(element: PsiElement, useFullName: Boolean) = ""
}