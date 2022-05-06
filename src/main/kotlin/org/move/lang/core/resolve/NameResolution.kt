package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.move.cli.MoveScope
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.infer.inferenceCtx
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.modules
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull
import org.move.stdext.chain

enum class MslScope {
    NONE, EXPR, LET, LET_POST;
}

val MvElement.mslScope: MslScope
    get() {
        if (!this.isMsl()) return MslScope.NONE
        val letStmt = this.ancestorOrSelf<MvLetStmt>()
        return when {
            letStmt == null -> MslScope.EXPR
            letStmt.isPost -> MslScope.LET_POST
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
        stopAfter = { it is MvModule || it is MvScript }
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
    if (resolved is MvUseAlias) {
        return (resolved.parent as MvModuleUseSpeck).fqModuleRef
    }
    if (resolved is MvUseItem && resolved.text == "Self") {
        return resolved.moduleImport().fqModuleRef
    }
    if (resolved !is MvModuleUseSpeck) return null

    return resolved.fqModuleRef
}

private fun processModules(
    fqModuleRef: MvFQModuleRef,
    file: MoveFile,
    processor: MatchingProcessor,
): Boolean {
    val moveProject = fqModuleRef.moveProject ?: return false
    val sourceAddress = fqModuleRef.addressRef.toAddress(moveProject)

    var resolved = false
    val visitor = object : MvVisitor() {
        override fun visitModule(mod: MvModule) {
            if (resolved) return
            val modAddress = mod.address()?.toAddress(moveProject)
            if (modAddress == sourceAddress) {
                resolved = processor.match(mod)
            }
        }
    }
    val modules = file.modules()
    for (module in modules) {
        if (resolved) break
        module.accept(visitor)
    }
    return resolved
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
    moveProject.processModuleFiles(MoveScope.MAIN) { moduleFile ->
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
    return when (itemVis.namespaces.single()) {
        Namespace.DOT_ACCESSED_FIELD -> {
            val dotExpr = scope as? MvDotExpr ?: return false

            val ctx = dotExpr.inferenceCtx(dotExpr.isMsl())
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
                return processor.matchAll(schema.fieldBindings)
            }
            false
        }
        Namespace.NAME -> when (scope) {
            is MvModuleBlock -> {
                processor.matchAll(
                    scope.itemImportNames(),
                )
            }
            is MvModule -> {
                processor.matchAll(
//                    scope.itemImportsWithoutAliases(),
//                    scope.itemImportsAliases(),
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
            is MvScriptBlock -> processor.matchAll(scope.itemImportNames())
            is MvScript -> processor.matchAll(
                listOf(
//                    scope.itemImports(),
//                    scope.itemImportsWithoutAliases(),
//                    scope.itemImportsAliases(),
                    scope.constBindings(),
                ).flatten(),
            )
            is MvFunctionLike -> processor.matchAll(scope.parameterBindings)
            is MvCodeBlock -> {
                val precedingLetDecls = scope.letStmts
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
            is MvItemSpec -> {
                val item = scope.item
                when (item) {
                    is MvFunction -> processor.matchAll(item.parameterBindings)
                    is MvStruct -> processor.matchAll(item.fields)
                    else -> false
                }
            }
            is MvSchema -> processor.matchAll(scope.fieldBindings)
            is MvQuantBindingsOwner -> processor.matchAll(scope.bindings)
            is MvSpecBlock -> {
                val visibleLetDecls = when (itemVis.msl) {
                    MslScope.EXPR -> scope.letStmts()
                    MslScope.LET, MslScope.LET_POST -> {
                        val letDecls = if (itemVis.msl == MslScope.LET_POST) {
                            scope.letStmts()
                        } else {
                            scope.letStmts(false)
                        }
                        letDecls
                            // drops all let-statements after the current position
//                            .filter { PsiUtilCore.compareElementsByPosition(it, cameFrom) <= 0 }
                            .filter { it.cameBefore(cameFrom) }
                            // drops let-statement that is ancestors of ref (on the same statement, at most one)
                            .filter { cameFrom != it && !PsiTreeUtil.isAncestor(cameFrom, it, true) }
                    }
                    MslScope.NONE -> emptyList()
                }
                // shadowing support (look at latest first)
                val namedElements = visibleLetDecls
                    .asReversed()
                    .flatMap { it.pat?.bindings.orEmpty() }
                    // add inline functions
                    .chain(scope.inlineFunctions().asReversed())

                // skip shadowed (already visited) elements
                val visited = mutableSetOf<String>()
                val processorWithShadowing = MatchingProcessor { entry ->
                    ((entry.name !in visited)
                            && processor.match(entry).also { visited += entry.name })
                }
                return processorWithShadowing.matchAll(namedElements.asIterable())
            }
            else -> false
        }
        Namespace.TYPE -> when (scope) {
            is MvFunctionLike -> processor.matchAll(scope.typeParameters)
            is MvStruct -> processor.matchAll(scope.typeParameters)
            is MvSchema -> processor.matchAll(scope.typeParams)
            is MvItemSpec -> {
                val funcItem = scope.funcItem
                if (funcItem != null) {
                    processor.matchAll(funcItem.typeParameters)
                } else {
                    false
                }
            }
            is MvModuleBlock -> processor.matchAll(scope.itemImportNames())
            is MvModule -> processor.matchAll(
                listOf(
//                    scope.itemImports(),
//                    scope.itemImportsWithoutAliases(),
//                    scope.itemImportsAliases(),
                    scope.structs(),
                ).flatten(),
            )
            is MvScriptBlock -> processor.matchAll(scope.itemImportNames())
//            is MvScriptDef -> processor.matchAll(
//                listOf(
////                    scope.itemImports(),
////                    scope.itemImportsWithoutAliases(),
////                    scope.itemImportsAliases(),
//                ).flatten(),
//            )
            is MvApplySchemaStmt -> {
                val toPatterns = scope.applyTo?.functionPatternList.orEmpty()
                val patternTypeParams = toPatterns.flatMap { it.typeParameters }
                processor.matchAll(patternTypeParams)
            }
            else -> false
        }
        Namespace.SPEC_ITEM -> when (scope) {
            is MvModule -> processor.matchAll(
                listOf(
                    scope.allFunctions(),
                    scope.structs(),
                ).flatten()
            )
            else -> false
        }
        Namespace.SCHEMA -> when (scope) {
            is MvModule -> processor.matchAll(scope.schemas())
            else -> false
        }
        Namespace.MODULE -> when (scope) {
            is MvItemsOwner -> processor.matchAll(
                listOf(
                    scope.moduleImportNames(),
//                    scope.moduleImportsWithoutAliases(),
//                    scope.moduleImportAliases(),
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
        // walk all `spec module {}` clauses
        if (cameFrom is MvAnySpec && scope is MvModuleBlock) {
            val moduleSpecs = (scope.parent as MvModule)
                .moduleSpecs()
                .filter { it != cameFrom }
            for (moduleSpec in moduleSpecs) {
                val moduleSpecBlock = moduleSpec.specBlock ?: continue
                if (handleScope(cameFrom, moduleSpecBlock)) return true
            }
        }
        if (handleScope(cameFrom, scope)) return true
        if (stopAfter(scope)) break

        cameFrom = scope
        scope = scope.parent as? MvElement
    }

    return false
}
