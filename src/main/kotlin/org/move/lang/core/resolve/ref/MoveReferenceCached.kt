package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import org.move.lang.core.psi.MvNamedElement

abstract class MvReferenceCached<T : MvReferenceElement>(element: T) : MvReferenceBase<T>(element) {
    abstract fun resolveInner(): List<MvNamedElement>

    final override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        cachedMultiResolve().toTypedArray()

    final override fun multiResolve(): List<MvNamedElement> =
        cachedMultiResolve().mapNotNull { it.element as? MvNamedElement }

    private fun cachedMultiResolve(): List<PsiElementResolveResult> {
        return MvResolveCache
            .getInstance(element.project)
            .resolveWithCaching(element, cacheDependency, Resolver)
            .orEmpty()
    }

    protected open val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE

    private object Resolver : (MvReferenceElement) -> List<PsiElementResolveResult> {
        override fun invoke(ref: MvReferenceElement): List<PsiElementResolveResult> {
            return (ref.reference as MvReferenceCached<*>).resolveInner().map { PsiElementResolveResult(it) }
        }
    }

//    private object Resolver :
//        ResolveCache.AbstractResolver<MvReferenceCached<*>, List<PsiElementResolveResult>> {
//        override fun resolve(
//            ref: MvReferenceCached<*>,
//            incompleteCode: Boolean
//        ): List<PsiElementResolveResult> {
//            return ref.resolveInner().map { PsiElementResolveResult(it) }
//        }
//    }
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
