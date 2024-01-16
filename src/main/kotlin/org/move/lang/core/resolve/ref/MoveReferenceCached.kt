package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import org.move.lang.core.psi.MvNamedElement

abstract class MvPolyVariantReferenceCached<T: MvReferenceElement>(element: T):
    MvPolyVariantReferenceBase<T>(element) {

    abstract fun multiResolveInner(): List<MvNamedElement>

    open fun multiResolveInnerResolveResults(): List<PsiElementResolveResult> =
        multiResolveInner()
            .map { PsiElementResolveResult(it, true) }

    final override fun multiResolve(): List<MvNamedElement> =
        multiResolveWithCaching()
            .filter { it.isValidResult }
            .mapNotNull { it.element as? MvNamedElement }

    /// incompleteCode allows for invalid results
    final override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        multiResolveWithCaching().toTypedArray()

    /// caching

    private fun multiResolveWithCaching(): List<PsiElementResolveResult> {
        return MvResolveCache
            .getInstance(element.project)
            .resolveWithCaching(element, cacheDependency, Resolver)
            .orEmpty()
    }

    protected open val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE

    private object Resolver: (MvReferenceElement) -> List<PsiElementResolveResult> {
        override fun invoke(ref: MvReferenceElement): List<PsiElementResolveResult> {
            return (ref.reference as MvPolyVariantReferenceCached<*>)
                .multiResolveInnerResolveResults()
        }
    }
}
