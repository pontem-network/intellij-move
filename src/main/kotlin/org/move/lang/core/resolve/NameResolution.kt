package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.descendantsOfType
import org.move.cli.GlobalScope
import org.move.lang.MvFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.infer.inferenceCtx
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull

enum class MslScope {
    NONE, EXPR, LET, LET_POST;
}

val MvElement.mslScope: MslScope
    get() {
        if (!this.isMslAvailable()) return MslScope.NONE
        val letStatement = this.ancestorOrSelf<MvLetStatement>()
        return when {
            letStatement == null -> MslScope.EXPR
            letStatement.isPost -> MslScope.LET_POST
            else -> MslScope.LET
        }
    }

data class ItemVis(
    val namespaces: Set<Namespace> = emptySet(),
    val visibilities: Set<Visibility> = emptySet(),
    val msl: MslScope = MslScope.NONE
) {
    val isMsl get() = msl != MslScope.NONE

    fun replace(
        ns: Set<Namespace> = this.namespaces,
        vs: Set<Visibility> = this.visibilities,
        msl: MslScope = this.msl
    ): ItemVis {
        return ItemVis(ns, vs, msl)
    }
}

fun processItems(
    element: MvElement,
    itemVis: ItemVis,
    processor: MatchingProcessor,
): Boolean {
    return walkUpThroughScopes(
        element,
        stopAfter = { it is MvModuleDef || it is MvScriptDef }
    ) { cameFrom, scope ->
        processLexicalDeclarations(
            scope, cameFrom, itemVis, processor
        )
    }
}

fun resolveSingleItem(element: MvReferenceElement, namespace: Namespace): MvNamedElement? {
    return resolveItem(element, namespace).firstOrNull()
}

fun resolveItem(element: MvReferenceElement, namespace: Namespace): List<MvNamedElement> {
    val itemVis = ItemVis(setOf(namespace), msl = element.mslScope)
    var resolved: MvNamedElement? = null
    processItems(element, itemVis) {
        if (it.name == element.referenceName) {
            resolved = it.element
            return@processItems true
        }
        return@processItems false
    }
    return resolved.wrapWithList()
}

fun resolveIntoFQModuleRef(moduleRef: MvModuleRef): MvFQModuleRef? {
    if (moduleRef is MvFQModuleRef) {
        return moduleRef
    }
    // module refers to ModuleImport
    val resolved = resolveSingleItem(moduleRef, Namespace.MODULE)
    if (resolved is MvImportAlias) {
        return (resolved.parent as MvModuleImport).fqModuleRef
    }
    if (resolved is MvItemImport && resolved.text == "Self") {
        return resolved.moduleImport().fqModuleRef
    }
    if (resolved !is MvModuleImport) return null

    return resolved.fqModuleRef
}

private fun processModules(
    fqModuleRef: MvFQModuleRef,
    file: MvFile,
    processor: MatchingProcessor,
): Boolean {
    val moveProject = fqModuleRef.moveProject ?: return false
    val sourceAddress = fqModuleRef.addressRef.toAddress(moveProject)

    var stop = false
    val visitor = object : MvVisitor() {
        override fun visitModuleDef(mod: MvModuleDef) {
            if (stop) return
            val modAddress = mod.definedAddressRef()?.toAddress(moveProject)
            if (modAddress == sourceAddress) {
                stop = processor.match(mod)
            }
        }
    }
    val modules = file.descendantsOfType<MvModuleDef>()
    for (module in modules) {
        module.accept(visitor)
    }
    return stop
}

fun processFQModuleRef(
    fqModuleRef: MvFQModuleRef,
    processor: MatchingProcessor,
) {
    // first search modules in the current file
    val currentFile = fqModuleRef.containingMoveFile ?: return
    var stopped = processModules(fqModuleRef, currentFile, processor)
    if (stopped) return

    val moveProject = currentFile.moveProject ?: return
    moveProject.processModuleFiles(GlobalScope.MAIN) { moduleFile ->
        // skip current file as it's processed already
        if (moduleFile.file.toNioPathOrNull() == currentFile.toNioPathOrNull())
            return@processModuleFiles true
        stopped = processModules(fqModuleRef, moduleFile.file, processor)
        // if not resolved, returns true to indicate that next file should be tried
        !stopped
    }
}

fun processLexicalDeclarations(
    scope: MvElement,
    cameFrom: MvElement,
    itemVis: ItemVis,
    processor: MatchingProcessor,
): Boolean {
    check(cameFrom.parent == scope)

    return when (itemVis.namespaces.single()) {
        Namespace.DOT_ACCESSED_FIELD -> {
            val dotExpr = scope as? MvDotExpr ?: return false

            val ctx = dotExpr.inferenceCtx
            val receiverTy = dotExpr.expr.inferExprTy(ctx)
            val innerTy = when (receiverTy) {
                is TyReference -> receiverTy.innerTy() as? TyStruct ?: TyUnknown
                is TyStruct -> receiverTy
                else -> TyUnknown
            }
            if (innerTy !is TyStruct) return false

            val fields = innerTy.item.fields
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
        Namespace.SCHEMA_FIELD -> {
            val schema = (scope as? MvSchemaLit)?.path?.maybeSchema
            if (schema != null) {
                return processor.matchAll(schema.declaredVars)
            }
            false
        }
        Namespace.NAME -> when (scope) {
            is MvModuleDef -> {
                processor.matchAll(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.allFunctions(),
                    scope.builtinFunctions(),
                    scope.structs(),
                    scope.constBindings(),
                    if (itemVis.isMsl) {
                        listOf(scope.specFunctions(), scope.builtinSpecFunctions()).flatten()
                    } else {
                        emptyList()
                    }
                )
            }
            is MvScriptDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.constBindings(),
//                    scope.builtinFunctions(),
                ).flatten(),
            )
            is MvFunction -> processor.matchAll(scope.parameterBindings)
            is MvSpecFunction -> processor.matchAll(scope.parameterBindings)
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
            is MvNameSpecDef -> {
                val item = scope.item
                when (item) {
                    is MvFunction -> processor.matchAll(item.parameterBindings)
                    else -> false
                }
            }
            is MvSchema -> processor.matchAll(scope.specBlock?.schemaVars().orEmpty())
            is MvSpecBlock -> {
                val visibleLetDecls = when (itemVis.msl) {
                    MslScope.EXPR -> scope.letStatements()
                    MslScope.LET, MslScope.LET_POST -> {
                        val letDecls = if (itemVis.msl == MslScope.LET_POST) {
                            scope.letStatements()
                        } else {
                            scope.letStatements(false)
                        }
                        letDecls
                            // drops all let-statements after the current position
                            .filter { PsiUtilCore.compareElementsByPosition(it, cameFrom) <= 0 }
                            // drops let-statement that is ancestors of ref (on the same statement, at most one)
                            .filter { cameFrom != it && !PsiTreeUtil.isAncestor(cameFrom, it, true) }
                    }
                    MslScope.NONE -> emptyList()
                }
                // shadowing support (look at latest first)
                val namedElements = visibleLetDecls
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
            is MvFunction -> processor.matchAll(scope.typeParameters)
            is MvSpecFunction -> processor.matchAll(scope.typeParameters)
            is MvStruct -> processor.matchAll(scope.typeParameters)
            is MvSchema -> processor.matchAll(scope.typeParams)
            is MvNameSpecDef -> {
                val funcItem = scope.funcItem
                if (funcItem != null) {
                    processor.matchAll(funcItem.typeParameters)
                } else {
                    false
                }
            }
            is MvModuleDef -> processor.matchAll(
                listOf(
                    scope.itemImportsWithoutAliases(),
                    scope.itemImportsAliases(),
                    scope.structs(),
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
        Namespace.SPEC_ITEM -> when (scope) {
            is MvModuleDef -> processor.matchAll(
                listOf(
                    scope.allFunctions(),
                    scope.structs(),
                ).flatten()
            )
            else -> false
        }
        Namespace.SCHEMA -> when (scope) {
            is MvModuleDef -> processor.matchAll(scope.schemas())
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
