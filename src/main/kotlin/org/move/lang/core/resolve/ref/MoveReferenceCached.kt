package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveReferenceElement

abstract class MoveReferenceCached<T : MoveReferenceElement>(element: T) : MoveReferenceBase<T>(element) {
    abstract fun resolveInner(): MoveNamedElement?

    override fun resolve(): MoveNamedElement? {
        return resolveWithCache()?.element as? MoveNamedElement
    }

    private fun resolveWithCache() =
        ResolveCache
            .getInstance(element.project)
            .resolveWithCaching(this, Resolver, true, false)

    private object Resolver : ResolveCache.AbstractResolver<MoveReferenceCached<*>, PsiElementResolveResult> {
        override fun resolve(
            ref: MoveReferenceCached<*>,
            incompleteCode: Boolean,
        ): PsiElementResolveResult? = ref.resolveInner()?.let { PsiElementResolveResult(it) }

    }
}

//abstract class MoveReferenceCached<T : MoveReferenceElement>(
//    element: T,
//) : MoveReferenceBase<T>(element) {
//
//    abstract fun resolveInner(): List<MoveNamedElement>
//
//    override fun resolve(): MoveNamedElement? {
//        return cachedMultiResolve().firstOrNull() as MoveNamedElement?
//    }
//
//    private fun cachedMultiResolve(): List<PsiElementResolveResult> {
//        return ResolveCache
//            .getInstance(element.project)
//            .resolveWithCaching(this, Resolver, true, false)
//            .orEmpty()
//    }
//
//    private object Resolver : ResolveCache.AbstractResolver<MoveReferenceCached<*>, List<PsiElementResolveResult>> {
//        override fun resolve(
//            ref: MoveReferenceCached<*>,
//            incompleteCode: Boolean,
//        ): List<PsiElementResolveResult> = ref.resolveInner().map { PsiElementResolveResult(it) }
//
//    }
//}
