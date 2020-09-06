package org.move.lang.core.resolve

import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.resolve.ref.Namespace

object ResolveEngine {
    open class ResolveResult private constructor(private val resolved: MoveNamedElement?) : com.intellij.psi.ResolveResult {
        companion object {
            fun buildFrom(candidates: Collection<MoveNamedElement>): ResolveResult {
                return when (candidates.count()) {
                    1 -> Resolved(candidates.first())
                    0 -> Unresolved
                    else -> Ambiguous(candidates)
                }
            }
        }

        override fun getElement(): MoveNamedElement? = resolved
        override fun isValidResult(): Boolean = resolved != null

        // Failure to resolve
        object Unresolved : ResolveResult(null)

        // More than one resolution result
        class Ambiguous(val candidates: Collection<MoveNamedElement>) : ResolveResult(null)

        // Successfully resolved
        class Resolved(resolved: MoveNamedElement) : ResolveResult(resolved)
    }

    fun resolve(ref: MoveReferenceElement, namespace: Namespace): ResolveResult {
        val candidates = mutableListOf<MoveNamedElement>()
        processNestedScopesUpwards(ref, namespace) {
            if (it.name == ref.referenceName && it.element != null) {
                candidates += it.element
                true
            } else {
                false
            }
        }

        return ResolveResult.buildFrom(candidates)
    }
}