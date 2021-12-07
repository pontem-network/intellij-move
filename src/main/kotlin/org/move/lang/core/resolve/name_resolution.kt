package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.descendantsOfType
import org.move.cli.GlobalScope
import org.move.lang.MvFile
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
    element: MvReferenceElement,
    namespace: Namespace,
    processor: MatchingProcessor,
): Boolean {
    return walkUpThroughScopes(
        element,
        stopAfter = { it is MvModuleDef || it is MvScriptDef }
    ) { cameFrom, scope ->
        processLexicalDeclarations(
            scope, cameFrom, namespace, processor
        )
    }
}


fun resolveItem(element: MvReferenceElement, namespace: Namespace): MvNamedElement? {
    var resolved: MvNamedElement? = null
    processItems(element, namespace) {
        if (it.name == element.referenceName && it.element != null) {
            resolved = it.element
            return@processItems true
        }
        return@processItems false
    }
    return resolved
}

fun resolveIntoFQModuleRef(moduleRef: MvModuleRef): MvFQModuleRef? {
    if (moduleRef is MvFQModuleRef) {
        return moduleRef
    }
    // module refers to ModuleImport
    val resolved = resolveItem(moduleRef, Namespace.MODULE)
    if (resolved is MvImportAlias) {
        return (resolved.parent as MvModuleImport).fqModuleRef
    }
    if (resolved is MvItemImport && resolved.text == "Self") {
        return resolved.moduleImport().fqModuleRef
    }
    if (resolved !is MvModuleImport) return null

    return resolved.fqModuleRef
}

private fun processQualModuleRefInFile(
    qualModuleRef: MvFQModuleRef,
    file: MvFile,
    processor: MatchingProcessor,
): Boolean {
    val moveProject = qualModuleRef.moveProject() ?: return false
    val sourceNormalizedAddress = qualModuleRef.addressRef.toNormalizedAddress(moveProject)

    var resolved = false
    val visitor = object : MvVisitor() {
        override fun visitModuleDef(o: MvModuleDef) {
            if (resolved) return
            val normalizedAddress = o.definedAddressRef()?.toNormalizedAddress(moveProject)
            if (normalizedAddress == sourceNormalizedAddress) {
                resolved = processor.match(o)
            }
        }
    }
    val moduleDefs = file.descendantsOfType<MvModuleDef>()
    for (moduleDef in moduleDefs) {
        moduleDef.accept(visitor)
    }
    return resolved
}

fun processQualModuleRef(
    qualModuleRef: MvFQModuleRef,
    processor: MatchingProcessor,
) {
    // first search modules in the current file
    val containingFile = qualModuleRef.containingFile as? MvFile ?: return
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
    startElement: MvElement,
    namespace: Namespace,
    processor: MatchingProcessor,
) {
    walkUpThroughScopes(
        startElement,
        stopAfter = { it is MvModuleDef || it is MvScriptDef }
    ) { cameFrom, scope ->
        processLexicalDeclarations(
            scope, cameFrom, namespace, processor
        )
    }
}

fun processLexicalDeclarations(
    scope: MvElement,
    cameFrom: MvElement,
    namespace: Namespace,
    processor: MatchingProcessor,
): Boolean {
    check(cameFrom.parent == scope)

    return when (namespace) {
        Namespace.DOT_ACCESSED_FIELD -> {
            val dotExpr = scope as? MvDotExpr ?: return false

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
                is MvStructPat -> scope.path.maybeStruct
                is MvStructLitExpr -> scope.path.maybeStruct
                else -> null
            }
            if (struct != null) return processor.matchAll(struct.fields)
            false
        }
        Namespace.NAME -> when (scope) {
            is MvModuleDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.allFnSignatures(),
                    scope.builtinFnSignatures(),
                    scope.structSignatures(),
                    scope.constBindings(),
                ).flatten()
            )
            is MvScriptDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.constBindings(),
                    scope.builtinScriptFnSignatures(),
                ).flatten(),
            )
            is MvFunctionDef ->
                processor.matchAll(scope.functionSignature?.parameters.orEmpty().map { it.bindingPat })
            is MvCodeBlock -> {
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
            is MvFunctionDef -> processor.matchAll(scope.functionSignature?.typeParameters.orEmpty())
            is MvNativeFunctionDef -> processor.matchAll(scope.functionSignature?.typeParameters.orEmpty())
            is MvStructDef -> processor.matchAll(scope.structSignature.typeParameters)
            is MvSchemaSpecDef -> processor.matchAll(scope.typeParams)
            is MvModuleDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.structSignatures(),
                ).flatten(),
            )
            is MvScriptDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                ).flatten(),
            )
            else -> false
        }
        Namespace.SPEC -> when {
//            is MvModuleDef -> processor.matchAll(scope.schemas())
            else -> false
        }
        Namespace.MODULE -> when (scope) {
            is MvImportStatementsOwner -> processor.matchAll(
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
    start: MvElement,
    stopAfter: (MvElement) -> Boolean,
    handleScope: (cameFrom: MvElement, scope: MvElement) -> Boolean,
): Boolean {

    var cameFrom = start
    var scope = start.parent as MvElement?
    while (scope != null) {
        if (handleScope(cameFrom, scope)) return true
        if (stopAfter(scope)) break

        cameFrom = scope
        scope = scope.parent as? MvElement
    }

    return false
}
