package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvReferenceElement

abstract class MvReferenceCached<T : MvReferenceElement>(element: T) : MvReferenceBase<T>(element) {
    abstract fun resolveInner(): MvNamedElement?

    override fun resolve(): MvNamedElement? {
        return resolveWithCache()?.element as? MvNamedElement
    }

    private fun resolveWithCache() =
        ResolveCache
            .getInstance(element.project)
            .resolveWithCaching(this, Resolver, true, false)

    private object Resolver : ResolveCache.AbstractResolver<MvReferenceCached<*>, PsiElementResolveResult> {
        override fun resolve(
            ref: MvReferenceCached<*>,
            incompleteCode: Boolean,
        ): PsiElementResolveResult? = ref.resolveInner()?.let { PsiElementResolveResult(it) }

    }
}

//abstract class MvReferenceCached<T : MvReferenceElement>(
//    element: T,
//) : MvReferenceBase<T>(element) {
//
//    abstract fun resolveInner(): List<MvNamedElement>
//
//    override fun resolve(): MvNamedElement? {
//        return cachedMultiResolve().firstOrNull() as MvNamedElement?
//    }
//
//    private fun cachedMultiResolve(): List<PsiElementResolveResult> {
//        return ResolveCache
//            .getInstance(element.project)
//            .resolveWithCaching(this, Resolver, true, false)
//            .orEmpty()
//    }
//
//    private object Resolver : ResolveCache.AbstractResolver<MvReferenceCached<*>, List<PsiElementResolveResult>> {
//        override fun resolve(
//            ref: MvReferenceCached<*>,
//            incompleteCode: Boolean,
//        ): List<PsiElementResolveResult> = ref.resolveInner().map { PsiElementResolveResult(it) }
//
//    }
//}
