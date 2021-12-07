package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.descendantsOfType
import org.move.cli.GlobalScope
import org.move.lang.MoveFile
import org.move.lang.containingMoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull

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
            scope, cameFrom, namespace, processor
        )
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

fun resolveIntoFQModuleRef(moduleRef: MoveModuleRef): MoveFQModuleRef? {
    if (moduleRef is MoveFQModuleRef) {
        return moduleRef
    }
    // module refers to ModuleImport
    val resolved = resolveItem(moduleRef, Namespace.MODULE)
    if (resolved is MoveImportAlias) {
        return (resolved.parent as MoveModuleImport).fqModuleRef
    }
    if (resolved is MoveItemImport && resolved.text == "Self") {
        return resolved.moduleImport().fqModuleRef
    }
    if (resolved !is MoveModuleImport) return null

    return resolved.fqModuleRef
}

private fun processQualModuleRefInFile(
    qualModuleRef: MoveFQModuleRef,
    file: MoveFile,
    processor: MatchingProcessor,
): Boolean {
    val moveProject = qualModuleRef.moveProject() ?: return false
    val sourceNormalizedAddress = qualModuleRef.addressRef.toNormalizedAddress(moveProject)

    var resolved = false
    val visitor = object : MoveVisitor() {
        override fun visitModuleDef(o: MoveModuleDef) {
            if (resolved) return
            val normalizedAddress = o.definedAddressRef()?.toNormalizedAddress(moveProject)
            if (normalizedAddress == sourceNormalizedAddress) {
                resolved = processor.match(o)
            }
        }
    }
    val moduleDefs = file.descendantsOfType<MoveModuleDef>()
    for (moduleDef in moduleDefs) {
        moduleDef.accept(visitor)
    }
    return resolved
}

fun processQualModuleRef(
    qualModuleRef: MoveFQModuleRef,
    processor: MatchingProcessor,
) {
    // first search modules in the current file
    val containingFile = qualModuleRef.containingFile as? MoveFile ?: return
    var stopped = processQualModuleRefInFile(qualModuleRef, containingFile, processor)
    if (stopped) return

    val moveProject = containingFile.containingMoveProject() ?: return
    moveProject.processModuleFiles(GlobalScope.MAIN) { moduleFile ->
        // skip current file as it's processed already
        if (moduleFile.file.toNioPathOrNull() == containingFile.toNioPathOrNull())
            return@processModuleFiles true
        stopped = processQualModuleRefInFile(qualModuleRef, moduleFile.file, processor)
        // if not resolved, returns true to indicate that next file should be tried
        !stopped
    }
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
            scope, cameFrom, namespace, processor
        )
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
        Namespace.DOT_ACCESSED_FIELD -> {
            val dotExpr = scope as? MoveDotExpr ?: return false

            val receiverTy = dotExpr.expr.inferExprTy()
            val innerTy = when (receiverTy) {
                is TyReference -> receiverTy.innerTy() as? TyStruct ?: TyUnknown
                is TyStruct -> receiverTy
                else -> TyUnknown
            }
            if (innerTy !is TyStruct) return false

            val fields = innerTy.item.structDef.fields.orEmpty()
            return processor.matchAll(fields)
        }
        Namespace.STRUCT_FIELD -> {
            val struct = when (scope) {
                is MoveStructPat -> scope.path.maybeStruct
                is MoveStructLitExpr -> scope.path.maybeStruct
                else -> null
            }
            if (struct != null) return processor.matchAll(struct.fields)
            false
        }
        Namespace.NAME -> when (scope) {
            is MoveModuleDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.allFnSignatures(),
                    scope.builtinFnSignatures(),
                    scope.structSignatures(),
                    scope.constBindings(),
                ).flatten()
            )
            is MoveScriptDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.constBindings(),
                    scope.builtinScriptFnSignatures(),
                ).flatten(),
            )
            is MoveFunctionDef ->
                processor.matchAll(scope.functionSignature?.parameters.orEmpty().map { it.bindingPat })
            is MoveCodeBlock -> {
                val precedingLetDecls = scope.letStatements
                    // drops all let-statements after the current position
                    .filter { PsiUtilCore.compareElementsByPosition(it, cameFrom) <= 0 }
                    // drops let-statement that is ancestors of ref (on the same statement, at most one)
                    .filter { cameFrom != it && !PsiTreeUtil.isAncestor(cameFrom, it, true) }

                // shadowing support (look at latest first)
                val namedElements = precedingLetDecls
                    .asReversed()
                    .flatMap { it.pat?.bindings.orEmpty() }

                // skip shadowed (already visited) elements
                val visited = mutableSetOf<String>()
                val processorWithShadowing = MatchingProcessor { entry ->
                    ((entry.name !in visited)
                            && processor.match(entry).also { visited += entry.name })
                }
                return processorWithShadowing.matchAll(namedElements)
            }
            else -> false
        }
        Namespace.TYPE -> when (scope) {
            is MoveFunctionDef -> processor.matchAll(scope.functionSignature?.typeParameters.orEmpty())
            is MoveNativeFunctionDef -> processor.matchAll(scope.functionSignature?.typeParameters.orEmpty())
            is MoveStructDef -> processor.matchAll(scope.structSignature.typeParameters)
            is MoveSchemaSpecDef -> processor.matchAll(scope.typeParams)
            is MoveModuleDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.structSignatures(),
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
        Namespace.SPEC -> when {
//            is MoveModuleDef -> processor.matchAll(scope.schemas())
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
        scope = scope.parent as? MoveElement
    }

    return false
}
