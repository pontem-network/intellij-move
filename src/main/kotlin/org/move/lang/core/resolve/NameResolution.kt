package org.move.lang.core.resolve

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveVisitor
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
    val fullyQualifiedModuleRef =
        if (moduleRef !is MoveFullyQualifiedModuleRef) {
            val referredItem = resolveItem(moduleRef, Namespace.MODULE) ?: return null

            if (referredItem is MoveImportAlias) return referredItem
            if (referredItem is MoveItemImport && referredItem.text == "Self") {
                return referredItem
            }
            (referredItem as MoveModuleImport).fullyQualifiedModuleRef
        } else {
            moduleRef
        }
//    val resolvedModule = null
//    val resolvedModule = resolveFullyQualifiedModuleRef(fullyQualifiedModuleRef)
//    if (resolvedModule == null) {
    return (fullyQualifiedModuleRef.parent as? MoveModuleImport)
//    }
//    return resolvedModule
}

//fun resolveUnqualifiedModuleRef(moduleRef: MoveModuleRef): MoveNamedElement? {
//    val referredElement = resolveItem(moduleRef, Namespace.MODULE) ?: return null
//    if (referredElement is MoveImportAlias) {
//        return referredElement
//    }
//    val referredModuleImport = referredElement as MoveModuleImport;
//    return resolveFullyQualifiedModuleRef(referredModuleImport.fullyQualifiedModuleRef)
//        ?: referredElement
//}

fun resolveFullyQualifiedModuleRefInFile(
    moduleRef: MoveFullyQualifiedModuleRef,
    file: MoveFile,
): MoveModuleDef? {
    val moduleAddress = moduleRef.addressRef.address()
    val moduleName = moduleRef.referenceName
    val containingModuleName = moduleRef.containingModule?.name

    var resolved: MoveModuleDef? = null
    file.accept(
        object : MoveVisitor(),
                 PsiRecursiveVisitor {
            override fun visitFile(file: PsiFile) {
                file.acceptChildren(this)
            }

            override fun visitAddressDef(o: MoveAddressDef) {
                if (resolved != null) return

                if (o.address == moduleAddress) {
                    val modules = o.addressBlock?.moduleDefList
                        .orEmpty()
                        .filter { it.name != containingModuleName }
                    for (module in modules) {
                        module.accept(this)
                    }
                }
            }

            override fun visitModuleDef(moduleDef: MoveModuleDef) {
                if (resolved != null) return

                if (moduleDef.name == moduleName) {
                    resolved = moduleDef
                }
            }
        })
    return resolved
}

fun resolveFullyQualifiedModuleRef(moduleRef: MoveFullyQualifiedModuleRef): MoveModuleDef? {
    val file = moduleRef.containingFile as? MoveFile ?: return null
    return resolveFullyQualifiedModuleRefInFile(moduleRef, file)
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
        Namespace.STRUCT_FIELD -> {
            val structDef = (scope as? MoveQualTypeReferenceElement)?.referredStructDef
            if (structDef != null) {
                return processor.matchAll(structDef.fields)
            }
            false
        }
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
                    scope.consts()
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
                    scope.moduleImports(),
                    scope.moduleImportAliases(),
                    scope.selfItemImports(),
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
