package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiReference
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveElement
import org.move.lang.core.types.BoundElement

interface MoveReference : PsiReference {

    override fun getElement(): MoveElement

    override fun resolve(): MoveNamedElement?
}

interface MovePathReference: MoveReference {
    fun advancedResolve(): BoundElement<MoveNamedElement>?
}
