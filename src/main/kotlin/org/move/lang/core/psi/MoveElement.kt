package org.move.lang.core.psi

import com.intellij.psi.PsiElement
import org.move.lang.core.resolve.ref.MoveReference

interface MoveElement : PsiElement {
    override fun getReference(): MoveReference?
}