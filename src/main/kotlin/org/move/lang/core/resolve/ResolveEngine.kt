package org.move.lang.core.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.MoveReferenceKind

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

    fun resolve(ref: MoveReferenceElement, kind: MoveReferenceKind): ResolveResult {
        val scopes = enumerateScopesFor(ref)
        return when (kind) {
            MoveReferenceKind.NAME -> resolveIn(scopes, ref)
            MoveReferenceKind.TYPE -> resolveTypeIn(scopes, ref)
            MoveReferenceKind.SCHEMA -> resolveSchemaIn(scopes, ref)
        }
    }

//    fun nameDeclarations(scope: MoveResolveScope, pivot: PsiElement? = null): Sequence<RustNamedElement> =
//        nameDeclarations(scope, pivot).mapNotNull { it.element }

    fun enumerateScopesFor(element: PsiElement): Sequence<MoveResolveScope> =
        generateSequence(ResolveUtil.getResolveScopeFor(element)) { parent ->
            ResolveUtil.getResolveScopeFor(parent)
        }
}

fun nameDeclarations(scope: MoveResolveScope, ref: MoveReferenceElement): Sequence<ScopeEntry> {
    val declarations = mutableListOf<ScopeEntry>()
    scope.accept(object : MoveVisitor() {
        override fun visitCodeBlock(o: MoveCodeBlock) {
            val visibleLetExprs = o.statementExprList
                // shadowing support (look at latest first)
                .asReversed()
                .asSequence()
                .filterIsInstance<MoveLetExpr>()
                // drops all let-statements after the current position
                .dropWhile { PsiUtilCore.compareElementsByPosition(ref, it) < 0 }
                // drops let-statement that is ancestors of ref (on the same statement, at most one)
                .dropWhile { PsiTreeUtil.isAncestor(it, ref, true) }

            val allEntries =
                visibleLetExprs.flatMap { it.pat?.boundElements.orEmpty().asSequence() }
                    .scopeEntries

            // entries are in reverse order, so keep only one-per-name first encountered
            val declaredNames = hashSetOf<String>()
            val nonShadowedEntries = allEntries.filter {
                val res = it.name !in declaredNames
                declaredNames.add(it.name)
                res
            }

            declarations.addAll(nonShadowedEntries)
        }

        override fun visitFunctionDef(o: MoveFunctionDef) {
            if (o.contains(ref)) {
                val entries = o.params
                    .asSequence()
                    .scopeEntries
                declarations.addAll(entries)
            }
        }

        override fun visitModuleDef(o: MoveModuleDef) {
            if (o.contains(ref)) {
                val entries = o.definitions().asSequence().scopeEntries
                declarations.addAll(entries)
            }
        }
    })
    return declarations.asSequence()
}

fun typeDeclarations(scope: MoveResolveScope, ref: MoveReferenceElement): Sequence<ScopeEntry> {
    val declarations = mutableListOf<ScopeEntry>()
    scope.accept(object : MoveVisitor() {
        override fun visitFunctionDef(o: MoveFunctionDef) {
            if (o.contains(ref)) {
                val entries = o.typeParameters.asSequence().scopeEntries
                declarations.addAll(entries)
            }
        }

        override fun visitStructDef(o: MoveStructDef) {
            if (o.contains(ref)) {
                val entries = o.typeParams.asSequence().scopeEntries
                declarations.addAll(entries)
            }
        }

        override fun visitSchemaDef(o: MoveSchemaDef) {
            if (o.contains(ref)) {
                val entries = o.typeParams.asSequence().scopeEntries
                declarations.addAll(entries)
            }
        }

        override fun visitModuleDef(o: MoveModuleDef) {
            if (o.contains(ref)) {
                val entries = o.definitions()
                    .asSequence()
                    .filterIsInstance<MoveStructDef>()
                    .scopeEntries
                declarations.addAll(entries)
            }
        }
    })
    return declarations.asSequence()
}

fun schemaDeclarations(scope: MoveResolveScope, ref: MoveReferenceElement): Sequence<ScopeEntry> {
    val declarations = mutableListOf<ScopeEntry>()
    scope.accept(object : MoveVisitor() {
        override fun visitModuleDef(o: MoveModuleDef) {
            if (o.contains(ref)) {
                val entries = o.definitions()
                    .asSequence()
                    .filterIsInstance<MoveSchemaDef>()
                    .scopeEntries
                declarations.addAll(entries)
            }
        }
    })
    return declarations.asSequence()
}

private fun resolveIn(
    scopes: Sequence<MoveResolveScope>,
    ref: MoveReferenceElement
): ResolveEngine.ResolveResult =
    scopes
        .flatMap { nameDeclarations(it, ref) }
        .find { it.name == ref.referenceName }
        ?.element.asResolveResult()

private fun resolveTypeIn(
    scopes: Sequence<MoveResolveScope>,
    ref: MoveReferenceElement
): ResolveEngine.ResolveResult =
    scopes
        .flatMap { typeDeclarations(it, ref) }
        .find { it.name == ref.referenceName }
        ?.element.asResolveResult()

private fun resolveSchemaIn(
    scopes: Sequence<MoveResolveScope>,
    ref: MoveReferenceElement
): ResolveEngine.ResolveResult =
    scopes
        .flatMap { schemaDeclarations(it, ref) }
        .find { it.name == ref.referenceName }
        ?.element.asResolveResult()

class ScopeEntry private constructor(
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
