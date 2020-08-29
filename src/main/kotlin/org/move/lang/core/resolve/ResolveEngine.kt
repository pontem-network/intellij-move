package org.move.lang.core.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.statementExprList

object ResolveEngine {
    open class ResolveResult private constructor(val resolved: MoveNamedElement?) : com.intellij.psi.ResolveResult {
        companion object {
            fun buildFrom(candidates: Collection<MoveNamedElement>): ResolveResult {
                return when (candidates.count()) {
                    1 -> ResolveResult.Resolved(candidates.first())
                    0 -> ResolveResult.Unresolved
                    else -> ResolveResult.Ambiguous(candidates)
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

    fun resolve(ref: MoveReferenceElement): ResolveResult =
        resolveIn(enumerateScopesFor(ref), ref)

    private fun enumerateScopesFor(element: PsiElement): Sequence<MoveResolveScope> =
        generateSequence(ResolveUtil.getResolveScopeFor(element)) { parent ->
            ResolveUtil.getResolveScopeFor(parent)
        }

//    fun resolve(ref: MoveReferenceElement): ResolveResult = recursionGuard(ref, Computable {
//        ResolveResult.buildFrom(listOf())
//    }) ?: ResolveResult.Unresolved
}

private fun declarations(scope: MoveResolveScope, ref: MoveReferenceElement): Sequence<ScopeEntry> {
    val declarations = mutableListOf<ScopeEntry>()
    scope.accept(object : MoveVisitor() {
        override fun visitCodeBlock(o: MoveCodeBlock) {
            val visibleLetExprs = o.statementExprList
                .asSequence()
                .filterIsInstance<MoveLetExpr>()
                .dropWhile { PsiUtilCore.compareElementsByPosition(ref, it) < 0 }

            val allBoundElements = visibleLetExprs.flatMap { it.boundElements.asSequence() }

            declarations.addAll(allBoundElements.scopeEntries)
        }
    })
    return declarations.asSequence()
}

private fun resolveIn(
    scopes: Sequence<MoveResolveScope>,
    ref: MoveReferenceElement
): ResolveEngine.ResolveResult =
    scopes
        .flatMap { declarations(it, ref) }
        .find {
            it.name == ref.referenceName
        }
        ?.element.asResolveResult()

private class ScopeEntry private constructor(
    val name: String,
    private val thunk: Lazy<MoveNamedElement?>
) {
    val element: MoveNamedElement? by thunk

    companion object {
        fun of(name: String, element: MoveNamedElement): ScopeEntry = ScopeEntry(name, lazyOf(element))

        fun of(element: MoveNamedElement): ScopeEntry? = element.name?.let { ScopeEntry.of(it, element) }

        fun lazy(name: String?, thunk: () -> MoveNamedElement?): ScopeEntry? = name?.let {
            ScopeEntry(name, lazy(thunk))
        }
    }

    override fun toString(): String {
        return "ScopeEntryImpl(name='$name', thunk=$thunk)"
    }
}

private val Sequence<MoveNamedElement>.scopeEntries: Sequence<ScopeEntry>
    get() = mapNotNull { ScopeEntry.of(it) }

private fun MoveNamedElement?.asResolveResult(): ResolveEngine.ResolveResult =
    if (this == null)
        ResolveEngine.ResolveResult.Unresolved
    else
        ResolveEngine.ResolveResult.Resolved(this)