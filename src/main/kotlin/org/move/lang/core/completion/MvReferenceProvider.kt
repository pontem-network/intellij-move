package org.move.lang.core.completion

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.move.lang.core.psi.MvBlock
import org.move.lang.core.psi.MvBlockExpr
import org.move.lang.core.psi.MvModuleDef

class MvReferenceProvider: PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        return arrayOf()
    }
}