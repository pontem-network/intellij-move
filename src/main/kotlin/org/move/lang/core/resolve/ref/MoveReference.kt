package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiPolyVariantReference
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement

interface MvReference : PsiPolyVariantReference {

    override fun getElement(): MvElement

    override fun resolve(): MvNamedElement?

    fun multiResolve(): List<MvNamedElement>
}

interface MvPathReference: MvReference
