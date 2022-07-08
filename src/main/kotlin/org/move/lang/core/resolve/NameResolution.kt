package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
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

enum class ItemScope {
    MAIN, TEST;
}

enum class FolderScope {
    SOURCES, TESTS;
}

fun MvElement.visibleInScope(itemScope: ItemScope): Boolean {
    return itemScope == ItemScope.TEST
            || this.itemScope == ItemScope.MAIN
}

fun MvElement.visibleInScope(folderScope: FolderScope): Boolean {
    return folderScope == FolderScope.TESTS
            || this.folderScope == FolderScope.SOURCES
}

fun MvElement.isVisibleInScopes(itemVis: ItemVis): Boolean =
    this.visibleInScope(itemVis.itemScope)
            && this.visibleInScope(itemVis.folderScope)

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
    val namespaces: Set<Namespace>,
    val visibilities: Set<Visibility>,
    val msl: MslScope,
    val itemScope: ItemScope,
    val folderScope: FolderScope
) {
    val isMsl get() = msl != MslScope.NONE

    fun replace(
        ns: Set<Namespace> = this.namespaces,
        vs: Set<Visibility> = this.visibilities,
        msl: MslScope = this.msl,
        itemScope: ItemScope = this.itemScope,
        folderScope: FolderScope = this.folderScope,
    ): ItemVis {
        return ItemVis(ns, vs, msl, itemScope, folderScope)
    }

    companion object {
        fun default(): ItemVis {
            return ItemVis(
                Namespace.none(),
                Visibility.none(),
                msl = MslScope.NONE,
                itemScope = ItemScope.MAIN,
                folderScope = FolderScope.SOURCES
            )
        }
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

fun resolveSingleItem(element: MvReferenceElement, namespaces: Set<Namespace>): MvNamedElement? {
    return resolveLocalItem(element, namespaces).firstOrNull()
}

fun resolveLocalItem(
    element: MvReferenceElement,
    namespaces: Set<Namespace>
): List<MvNamedElement> {
    val itemVis = ItemVis(
        namespaces,
        msl = element.mslScope,
        visibilities = Visibility.local(),
        itemScope = element.itemScope,
        folderScope = element.folderScope
    )
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
    val resolved = resolveSingleItem(moduleRef, setOf(Namespace.MODULE))
    if (resolved is MvUseAlias) {
        return (resolved.parent as MvModuleUseSpeck).fqModuleRef
    }
    if (resolved is MvUseItem && resolved.text == "Self") {
        return resolved.moduleImport().fqModuleRef
    }
    if (resolved !is MvModuleUseSpeck) return null

    return resolved.fqModuleRef
}

fun processFileItems(
    file: MoveFile,
    itemVis: ItemVis,
    processor: MatchingProcessor,
): Boolean {
    for (module in file.modules()) {
        for (namespace in itemVis.namespaces) {
            val found = when (namespace) {
                Namespace.MODULE -> processor.match(itemVis, module)
                Namespace.NAME -> {
                    val functions = itemVis.visibilities.flatMap { module.functions(it) }
                    val specFunctions = if (itemVis.isMsl) module.specFunctions() else emptyList()
                    val consts = if (itemVis.isMsl) module.constBindings() else emptyList()
                    processor.matchAll(
                        itemVis,
                        functions, specFunctions, consts
                    )
                }
                Namespace.TYPE -> processor.matchAll(itemVis, module.structs())
                Namespace.SCHEMA -> processor.matchAll(itemVis, module.schemas())
                else -> continue
            }
            if (found) return true
        }
    }
    return false
}

fun processFQModuleRef(
    fqModuleRef: MvFQModuleRef,
    processor: MatchingProcessor,
) {
    val itemVis = ItemVis.default().replace(
        ns = setOf(Namespace.MODULE),
        itemScope = fqModuleRef.itemScope,
        folderScope = fqModuleRef.folderScope
    )
    val moveProject = fqModuleRef.moveProject ?: return
    val refAddress = fqModuleRef.addressRef.toAddress(moveProject)

    val moduleProcessor = MatchingProcessor {
        val module = it.element as MvModule
        val modAddress = module.address()?.toAddress(moveProject)
        if (modAddress != refAddress) return@MatchingProcessor false
        if (modAddress == refAddress) {
            return@MatchingProcessor processor.match(it)
        }
        false
    }

    // first search modules in the current file
    val currentFile = fqModuleRef.containingMoveFile ?: return
    var stopped = processFileItems(currentFile, itemVis, moduleProcessor)
    if (stopped) return

    moveProject.processMoveFiles { moveFile ->
        // skip current file as it's processed already
        if (moveFile.toNioPathOrNull() == currentFile.toNioPathOrNull())
            return@processMoveFiles true
        stopped = processFileItems(moveFile, itemVis, moduleProcessor)
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
    for (namespace in itemVis.namespaces) {
        val stop = when (namespace) {
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

                val structItem = innerTy.item
                if (structItem.containingModule != dotExpr.containingModule) return false

                val fields = structItem.fields
                return processor.matchAll(itemVis, fields)
            }
            Namespace.STRUCT_FIELD -> {
                val struct = when (scope) {
                    is MvStructPat -> scope.path.maybeStruct
                    is MvStructLitExpr -> scope.path.maybeStruct
                    else -> null
                }
                if (struct != null) return processor.matchAll(itemVis, struct.fields)
                false
            }
            Namespace.SCHEMA_FIELD -> {
                val schema = (scope as? MvSchemaLit)?.path?.maybeSchema
                if (schema != null) {
                    return processor.matchAll(itemVis, schema.fieldBindings)
                }
                false
            }
            Namespace.NAME -> when (scope) {
                is MvModuleBlock -> {
                    processor.matchAll(
                        itemVis,
                        scope.itemImportNames(),
                    )
                }
                is MvModule -> {
                    processor.matchAll(
                        itemVis,
                        scope.allNonTestFunctions(),
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
                is MvScriptBlock -> processor.matchAll(itemVis, scope.itemImportNames())
                is MvScript -> processor.matchAll(itemVis, scope.constBindings())
                is MvFunctionLike -> processor.matchAll(itemVis, scope.parameterBindings)
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
                    return processorWithShadowing.matchAll(itemVis, namedElements)
                }
                is MvItemSpec -> {
                    val item = scope.item
                    when (item) {
                        is MvFunction -> processor.matchAll(itemVis, item.parameterBindings)
                        is MvStruct -> processor.matchAll(itemVis, item.fields)
                        else -> false
                    }
                }
                is MvSchema -> processor.matchAll(itemVis, scope.fieldBindings)
                is MvQuantBindingsOwner -> processor.matchAll(itemVis, scope.bindings)
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
                    return processorWithShadowing.matchAll(
                        itemVis,
                        namedElements.asIterable()
                    )
                }
                else -> false
            }
            Namespace.TYPE -> when (scope) {
                is MvFunctionLike -> processor.matchAll(itemVis, scope.typeParameters)
                is MvStruct -> processor.matchAll(itemVis, scope.typeParameters)
                is MvSchema -> processor.matchAll(itemVis, scope.typeParams)
                is MvItemSpec -> {
                    val funcItem = scope.funcItem
                    if (funcItem != null) {
                        processor.matchAll(itemVis, funcItem.typeParameters)
                    } else {
                        false
                    }
                }
                is MvModuleBlock -> processor.matchAll(itemVis, scope.itemImportNames())
                is MvModule -> processor.matchAll(itemVis, scope.structs())
                is MvScriptBlock -> processor.matchAll(itemVis, scope.itemImportNames())
                is MvApplySchemaStmt -> {
                    val toPatterns = scope.applyTo?.functionPatternList.orEmpty()
                    val patternTypeParams = toPatterns.flatMap { it.typeParameters }
                    processor.matchAll(itemVis, patternTypeParams)
                }
                else -> false
            }
            Namespace.SPEC_ITEM -> when (scope) {
                is MvModule -> processor.matchAll(
                    itemVis,
                    listOf(
                        scope.allNonTestFunctions(),
                        scope.structs(),
                    ).flatten()
                )
                else -> false
            }
            Namespace.SCHEMA -> when (scope) {
                is MvModule -> processor.matchAll(itemVis, scope.schemas())
                else -> false
            }
            Namespace.MODULE -> when (scope) {
                is MvItemsOwner -> processor.matchAll(
                    itemVis,
                    listOf(
                        scope.moduleImportNames(),
                        scope.selfItemImports(),
                    ).flatten(),
                )
                else -> false
            }
        }
        if (stop) return true
    }
    return false
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
