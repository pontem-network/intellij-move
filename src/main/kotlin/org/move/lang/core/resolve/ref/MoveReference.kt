package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiReference
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.ext.MoveElement

interface MoveReference : PsiReference {
    override fun getElement(): MoveElement
    override fun resolve(): MoveNamedElement?
}