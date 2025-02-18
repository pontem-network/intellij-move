package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiPolyVariantReference
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement

interface MvPolyVariantReference : PsiPolyVariantReference {

    override fun getElement(): MvElement

    override fun resolve(): MvNamedElement?

    fun multiResolve(): List<MvNamedElement>

    fun resolveFollowingAliases(): MvNamedElement? = this.resolve()?.let { resolveAliases(it) }
}

//interface MvPathReference : MvPolyVariantReference {

//    fun multiResolveIfVisible(): List<MvElement> = multiResolve()
//
//    fun rawMultiResolve(): List<RsPathResolveResult<MvElement>> =
//        multiResolve().map { RsPathResolveResult(it, isVisible = true) }
//}

interface MvPath2Reference: MvPolyVariantReference {
//    fun multiResolveIfVisible(): List<MvElement> = multiResolve()

//    fun rawMultiResolve(): List<RsPathResolveResult<MvElement>>
//        multiResolve().map { RsPathResolveResult(it, isVisible = true) }
    
}
