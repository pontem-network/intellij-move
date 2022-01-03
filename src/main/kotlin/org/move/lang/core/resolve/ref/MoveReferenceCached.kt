package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvReferenceElement

abstract class MvReferenceCached<T : MvReferenceElement>(element: T) : MvReferenceBase<T>(element) {
    abstract fun resolveInner(): List<MvNamedElement>

    final override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        cachedMultiResolve().toTypedArray()

    final override fun multiResolve(): List<MvNamedElement> =
        cachedMultiResolve().mapNotNull { it.element as? MvNamedElement }

    private fun cachedMultiResolve(): List<PsiElementResolveResult> {
        return ResolveCache
            .getInstance(element.project)
            .resolveWithCaching(this, Resolver, true, false)
            .orEmpty()
    }

//    final override fun resolve(): MvNamedElement? {
//        return resolveWithCache()?.element as? MvNamedElement
//    }

//    private fun resolveWithCache() =
//        ResolveCache
//            .getInstance(element.project)
//            .resolveWithCaching(this, Resolver, true, false)

//    private object Resolver : ResolveCache.AbstractResolver<MvReferenceCached<*>, PsiElementResolveResult> {
//        override fun resolve(
//            ref: MvReferenceCached<*>,
//            incompleteCode: Boolean,
//        ): PsiElementResolveResult = ref.resolveInner().let { PsiElementResolveResult(it) }
//
//    }

    private object Resolver :
        ResolveCache.AbstractResolver<MvReferenceCached<*>, List<PsiElementResolveResult>> {
        override fun resolve(
            ref: MvReferenceCached<*>,
            incompleteCode: Boolean
        ): List<PsiElementResolveResult> {
            return ref.resolveInner().map { PsiElementResolveResult(it) }
        }
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
