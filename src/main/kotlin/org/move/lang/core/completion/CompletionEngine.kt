package org.move.lang.core.completion

import com.intellij.codeInsight.lookup.LookupElement
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.resolve.ResolveEngine
import org.move.lang.core.resolve.nameDeclarations
import org.move.lang.core.resolve.ref.MoveReferenceKind
import org.move.lang.core.resolve.schemaDeclarations
import org.move.lang.core.resolve.typeDeclarations

object CompletionEngine {
    fun complete(ref: MoveReferenceElement, kind: MoveReferenceKind): Array<out LookupElement> {
        val completions = when (kind) {
            MoveReferenceKind.NAME -> collectNameCompletions(ref)
            MoveReferenceKind.TYPE -> collectTypeCompletions(ref)
            MoveReferenceKind.SCHEMA -> collectSchemaCompletions(ref)
        }
        return completions.toVariantsArray()
    }

    private fun collectNameCompletions(ref: MoveReferenceElement): Collection<MoveNamedElement> {
        val scopes = ResolveEngine.enumerateScopesFor(ref)
        return scopes
            .flatMap { nameDeclarations(it, ref) }
            .mapNotNull { it.element }
            .toList()
    }

    private fun collectTypeCompletions(ref: MoveReferenceElement): Collection<MoveNamedElement> {
        val scopes = ResolveEngine.enumerateScopesFor(ref)
        return scopes
            .flatMap { typeDeclarations(it, ref) }
            .mapNotNull { it.element }
            .toList()
    }

    private fun collectSchemaCompletions(ref: MoveReferenceElement): Collection<MoveNamedElement> {
        val scopes = ResolveEngine.enumerateScopesFor(ref)
        return scopes
            .flatMap { schemaDeclarations(it, ref) }
            .mapNotNull { it.element }
            .toList()
    }

}

private fun Collection<MoveNamedElement>.toVariantsArray(): Array<out LookupElement> =
    this
        .filter { it.name != null }
        .map { it.createLookupElement() }
        .toTypedArray()
