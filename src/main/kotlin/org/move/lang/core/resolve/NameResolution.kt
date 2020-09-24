package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace


fun processItems(
    element: MoveReferenceElement,
    namespace: Namespace,
    processor: MatchingProcessor,
): Boolean {
    return walkUpThroughScopes(
        element,
        stopAfter = { it is MoveModuleDef || it is MoveScriptDef }
    ) { cameFrom, scope ->
        processLexicalDeclarations(
            scope, cameFrom, namespace, processor)
    }
}


fun resolveItem(element: MoveReferenceElement, namespace: Namespace): MoveNamedElement? {
    var resolved: MoveNamedElement? = null
    processItems(element, namespace) {
        if (it.name == element.referenceName && it.element != null) {
            resolved = it.element
            return@processItems true
        }
        return@processItems false
    }
    return resolved
}

fun resolveModuleRef(moduleRef: MoveModuleRef): MoveNamedElement? {
    return when (moduleRef) {
        is MoveFullyQualifiedModuleRef -> resolveExternalModule(moduleRef)
        else -> resolveUnqualifiedModuleRef(moduleRef)
    }
}

fun resolveUnqualifiedModuleRef(moduleRef: MoveModuleRef): MoveNamedElement? {
    val referredElement = resolveItem(moduleRef, Namespace.MODULE) ?: return null
    if (referredElement is MoveImportAlias) {
        return referredElement
    }
    return resolveExternalModule((referredElement as MoveModuleImport).fullyQualifiedModuleRef)
        ?: referredElement
}


fun resolveExternalModule(moduleRef: MoveFullyQualifiedModuleRef): MoveModuleDef? {
    val file = moduleRef.containingFile as? MoveFile ?: return null

    val address = moduleRef.addressRef.address()

    var resolved: MoveModuleDef? = null
    file.accept(object : MoveRecursiveVisitor() {
        override fun visitAddressDef(o: MoveAddressDef) {
            if (o.address == address) {
                for (module in o.modules()) {
                    if (module.name == moduleRef.referenceName) {
                        resolved = module
                    }
                }
            }
        }
    })
    return resolved
}

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
            is MoveSpecBlock -> {
                processor.matchAll(scope.defineFunctionList)
            }
            is MoveModuleDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.functions(),
                    scope.nativeFunctions(),
                    scope.structs(),
                    scope.nativeStructs(),
                    scope.consts(),
                ).flatten()
            )
            is MoveScriptDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                ).flatten(),
            )
            else -> false
        }
        Namespace.TYPE -> when (scope) {
            is MoveTypeParametersOwner -> processor.matchAll(scope.typeParameters)
            is MoveModuleDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.structs(),
                    scope.nativeStructs(),
                ).flatten(),
            )
            is MoveScriptDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                ).flatten(),
            )
            else -> false
        }
        Namespace.SCHEMA -> when (scope) {
            is MoveModuleDef -> processor.matchAll(scope.schemas())
            else -> false
        }
        Namespace.MODULE -> when (scope) {
            is MoveImportStatementsOwner -> processor.matchAll(
                listOf(
                    scope.moduleImportsWithoutAliases(),
                    scope.moduleImportAliases(),
                ).flatten(),
            )
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
