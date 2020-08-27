package org.move.lang.core.psi.ext

import com.intellij.psi.PsiPolyVariantReference

interface MoveReference : PsiPolyVariantReference {
    override fun getElement(): MoveElementImpl

    override fun resolve(): MoveElementImpl?

    fun multiResolve(): List<MoveElementImpl>
}