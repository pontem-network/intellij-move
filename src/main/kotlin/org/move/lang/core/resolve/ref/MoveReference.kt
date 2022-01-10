package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvElement
import org.move.lang.core.types.BoundElement

interface MvReference : PsiPolyVariantReference {

    override fun getElement(): MvElement

    override fun resolve(): MvNamedElement?

    fun multiResolve(): List<MvNamedElement>
}

interface MvPathReference: MvReference {
    fun advancedResolve(): BoundElement<MvNamedElement>?
}
