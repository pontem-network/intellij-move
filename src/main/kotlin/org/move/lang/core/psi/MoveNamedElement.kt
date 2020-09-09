package org.move.lang.core.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.ext.findLastChildByType

interface MoveNamedElement : MoveElement,
                             PsiNamedElement,
                             NavigatablePsiElement {
    val nameElement: PsiElement?
        get() = findLastChildByType(MoveElementTypes.IDENTIFIER)
}