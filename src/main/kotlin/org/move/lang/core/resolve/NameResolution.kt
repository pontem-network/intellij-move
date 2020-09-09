package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace

fun processNestedScopesUpwards(
    startElement: MoveElement,
    namespace: Namespace,
    processor: MatchingProcessor,
) {
    walkUpThroughScopes(
        startElement,
        stopAfter = { it is MoveModuleDef || it is MoveScriptDef }
    ) { cameFrom, scope ->
        processLexicalDeclarations(
            scope, cameFrom, namespace, processor)
    }

}

fun processLexicalDeclarations(
    scope: MoveElement,
    cameFrom: MoveElement,
    namespace: Namespace,
    processor: MatchingProcessor,
): Boolean {
    check(cameFrom.parent == scope)

    return when (namespace) {
        Namespace.NAME -> when (scope) {
            is MoveFunctionDef -> processor.matchAll(scope.params)
            is MoveCodeBlock -> {
                val precedingLetDecls = scope.statementExprList
                    .filterIsInstance<MoveLetExpr>()
                    // drops all let-statements after the current position
                    .filter { PsiUtilCore.compareElementsByPosition(it, cameFrom) <= 0 }
                    // drops let-statement that is ancestors of ref (on the same statement, at most one)
                    .filter { !PsiTreeUtil.isAncestor(cameFrom, it, true) }

                // shadowing support (look at latest first)
                val namedElements = precedingLetDecls
                    .asReversed()
                    .flatMap { it.pat?.boundElements.orEmpty() }

                // skip shadowed (already visited) elements
                val visited = mutableSetOf<String>()
                val processorWithShadowing = MatchingProcessor { entry ->
                    ((entry.name !in visited)
                            && processor.match(entry).also { visited += entry.name })
                }
                return processorWithShadowing.matchAll(namedElements)
            }
            is MoveModuleDef -> processor.matchAll(
                listOf(
                    scope.importAliases(),
                    scope.functions(),
                    scope.nativeFunctions(),
                    scope.structs(),
                    scope.nativeStructs(),
                    scope.consts(),
                ).flatten()
            )
            is MoveScriptDef -> processor.matchAll(scope.importAliases())
            else -> false
        }
        Namespace.TYPE -> when (scope) {
            is MoveTypeParametersOwner -> processor.matchAll(scope.typeParams)
            is MoveModuleDef -> processor.matchAll(
                listOf(
                    scope.importAliases(),
                    scope.structs(),
                    scope.nativeStructs()
                ).flatten())
            is MoveScriptDef -> processor.matchAll(scope.importAliases())
            else -> false
        }
        Namespace.SCHEMA -> when (scope) {
            is MoveModuleDef -> processor.matchAll(scope.schemas())
            else -> false
        }
    }
}

fun walkUpThroughScopes(
    start: MoveElement,
    stopAfter: (MoveElement) -> Boolean,
    handleScope: (cameFrom: MoveElement, scope: MoveElement) -> Boolean,
): Boolean {

    var cameFrom = start
    var scope = start.parent as MoveElement?
    while (scope != null) {
        if (handleScope(cameFrom, scope)) return true
        if (stopAfter(scope)) break

        cameFrom = scope
        scope = scope.parent as MoveElement?
    }

    return false
}
