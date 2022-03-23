package org.move.lang.core.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.findLastChildByType

interface MvNamedElement : MvElement,
                           PsiNamedElement,
                           NavigatablePsiElement {
    val nameElement: PsiElement?
        get() = findLastChildByType(MvElementTypes.IDENTIFIER)
}
