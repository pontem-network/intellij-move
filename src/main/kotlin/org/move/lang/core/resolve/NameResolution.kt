package org.move.lang.core.resolve

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.address
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.index.MvNamedElementIndex
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull
import org.move.stdext.chain
import org.move.stdext.wrapWithList

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
    val namespaces: Set<Namespace>,
    val visibilities: Set<Visibility>,
    val mslScope: MslScope,
    val itemScope: ItemScope,
) {
    val isMsl get() = mslScope != MslScope.NONE
}

fun processItems(
    element: MvElement,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    return walkUpThroughScopes(
        element,
        stopAfter = { it is MvModule }
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
        mslScope = element.mslScope,
        visibilities = Visibility.local(),
        itemScope = element.itemScope,
    )
    val referenceName = element.referenceName
    var resolved: MvNamedElement? = null
    processItems(element, itemVis) {
        if (it.name == referenceName) {
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
    var resolved = resolveSingleItem(moduleRef, setOf(Namespace.MODULE))
    if (resolved is MvUseAlias) {
        resolved = resolved.moduleUseSpeck ?: resolved.useItem
    }
    if (resolved is MvUseItem && resolved.isSelf) {
        return resolved.moduleImport().fqModuleRef
    }
    if (resolved !is MvModuleUseSpeck) return null

    return resolved.fqModuleRef
}

fun processQualItem(
    item: MvNamedElement,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    val matched = when {
        item is MvModule && Namespace.MODULE in itemVis.namespaces
                || item is MvStruct && Namespace.TYPE in itemVis.namespaces
                || item is MvSchema && Namespace.SCHEMA in itemVis.namespaces ->
            processor.match(itemVis, item)

        item is MvFunction && Namespace.NAME in itemVis.namespaces -> {
            if (item.isTest) return false
            for (vis in itemVis.visibilities) {
                when {
                    vis is Visibility.Public
                            && item.visibility == FunctionVisibility.PUBLIC -> processor.match(itemVis, item)

                    vis is Visibility.PublicScript
                            && item.visibility == FunctionVisibility.PUBLIC_SCRIPT ->
                        processor.match(itemVis, item)

                    vis is Visibility.PublicFriend && item.visibility == FunctionVisibility.PUBLIC_FRIEND -> {
                        val itemModule = item.module ?: return false
                        val currentModule = vis.currentModule.element ?: return false
                        if (currentModule.fqModule() in itemModule.declaredFriendModules) {
                            processor.match(itemVis, item)
                        }
                    }

                    vis is Visibility.Internal -> processor.match(itemVis, item)
                }
            }
            false
        }
        else -> false
    }
    return matched
}

fun processFileItems(
    file: MoveFile,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (module in file.modules()) {
        for (namespace in itemVis.namespaces) {
            val found = when (namespace) {
                Namespace.MODULE -> processor.match(itemVis, module)
                Namespace.NAME -> {
                    val functions = itemVis.visibilities.flatMap { module.visibleFunctions(it) }
                    val specFunctions = if (itemVis.isMsl) module.specFunctions() else emptyList()
                    val consts = if (itemVis.isMsl) module.consts() else emptyList()
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
    processor: MatchingProcessor<MvModule>,
) {
    val itemVis = ItemVis(
        namespaces = setOf(Namespace.MODULE),
        visibilities = Visibility.local(),
        mslScope = fqModuleRef.mslScope,
        itemScope = fqModuleRef.itemScope,
    )
    val moveProj = fqModuleRef.moveProject ?: return
    val refAddress = fqModuleRef.addressRef.address(moveProj)?.canonicalValue

    val moduleProcessor = MatchingProcessor<MvNamedElement> {
        val entry = SimpleScopeEntry(it.name, it.element as MvModule)
        val modAddress = entry.element.address(moveProj)?.canonicalValue
        if (modAddress != refAddress)
            return@MatchingProcessor false
        processor.match(entry)
    }

    // first search modules in the current file
    val currentFile = fqModuleRef.containingMoveFile ?: return
    var stopped = processFileItems(currentFile, itemVis, moduleProcessor)
    if (stopped) return

    moveProj.processMoveFiles { moveFile ->
        // skip current file as it's processed already
        if (moveFile.toNioPathOrNull() == currentFile.toNioPathOrNull())
            return@processMoveFiles true
        stopped = processFileItems(moveFile, itemVis, moduleProcessor)
        // if not resolved, returns true to indicate that next file should be tried
        !stopped
    }
}

fun processFQModuleRef(
    fqModuleRef: MvFQModuleRef,
    target: String,
    processor: MatchingProcessor<MvModule>,
) {
    val itemVis = ItemVis(
        namespaces = setOf(Namespace.MODULE),
        visibilities = Visibility.local(),
        mslScope = fqModuleRef.mslScope,
        itemScope = fqModuleRef.itemScope,
    )
    val project = fqModuleRef.project
    val moveProj = fqModuleRef.moveProject ?: return

    val refAddress = fqModuleRef.addressRef.address(moveProj)?.canonicalValue
    val moduleProcessor = MatchingProcessor<MvNamedElement> {
        val entry = SimpleScopeEntry(it.name, it.element as MvModule)
        // TODO: check belongs to the current project
        val modAddress = entry.element.address(moveProj)?.canonicalValue

        if (modAddress != refAddress) return@MatchingProcessor false
        processor.match(entry)
    }

    // first search modules in the current file
    val currentFile = fqModuleRef.containingMoveFile ?: return
    val stopped = processFileItems(currentFile, itemVis, moduleProcessor)
    if (stopped) return

    val currentFileScope = GlobalSearchScope.fileScope(currentFile)
    val searchScope =
        moveProj.searchScope().intersectWith(GlobalSearchScope.notScope(currentFileScope))

    MvNamedElementIndex
        .processElementsByName(project, target, searchScope) {
            val matched = processQualItem(it, itemVis, moduleProcessor)
            if (matched) return@processElementsByName false

            true
        }
}

fun processLexicalDeclarations(
    scope: MvElement,
    cameFrom: MvElement,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (namespace in itemVis.namespaces) {
        val stop = when (namespace) {
            Namespace.DOT_FIELD -> {
                val dotExpr = scope as? MvDotExpr ?: return false

                val receiverTy = dotExpr.expr.inferredExprTy()
                val innerTy = when (receiverTy) {
                    is TyReference -> receiverTy.innerTy() as? TyStruct ?: TyUnknown
                    is TyStruct -> receiverTy
                    else -> TyUnknown
                }
                if (innerTy !is TyStruct) return false

                val structItem = innerTy.item
                val dotExprModule = dotExpr.namespaceModule ?: return false
                if (structItem.containingModule != dotExprModule) return false

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
                val schema = (scope as? MvSchemaLitExpr)?.path?.maybeSchema
                if (schema != null) {
                    return processor.matchAll(itemVis, schema.fieldBindings)
                }
                false
            }

            Namespace.ERROR_CONST -> {
                if (scope is MvImportsOwner) {
                    if (processor.matchAll(itemVis, scope.itemImportNames())) return true
                }
                when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.matchAll(
                            itemVis,
                            module.consts(),
                        )
                    }
                    else -> false
                }
            }

            Namespace.NAME -> {
                if (scope is MvImportsOwner) {
                    if (processor.matchAll(itemVis, scope.itemImportNames())) return true
                }
                when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.matchAll(
                            itemVis,
                            module.allNonTestFunctions(),
                            module.builtinFunctions(),
                            module.structs(),
                            module.consts(),
                            if (itemVis.isMsl) {
                                listOf(module.specFunctions(), module.builtinSpecFunctions()).flatten()
                            } else {
                                emptyList()
                            }
                        )
                    }
                    is MvModuleSpecBlock -> processor.matchAll(
                        itemVis,
                        scope.schemaList,
                        scope.specFunctionList,
                    )
                    is MvScript -> processor.matchAll(itemVis, scope.consts())
                    is MvFunctionLike -> processor.matchAll(itemVis, scope.parameterBindings())
                    is MvLambdaExpr -> processor.matchAll(itemVis, scope.bindingPatList)
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
                        processorWithShadowing.matchAll(itemVis, namedElements)
                    }

                    is MvItemSpec -> {
                        val item = scope.item
                        when (item) {
                            is MvFunction -> processor.matchAll(itemVis, item.parameterBindings())
                            is MvStruct -> processor.matchAll(itemVis, item.fields)
                            else -> false
                        }
                    }
                    is MvSchema -> processor.matchAll(itemVis, scope.fieldBindings)
                    is MvQuantBindingsOwner -> processor.matchAll(itemVis, scope.bindings)
                    is MvItemSpecBlock -> {
                        val visibleLetDecls = when (itemVis.mslScope) {
                            MslScope.EXPR -> scope.letStmts()
                            MslScope.LET, MslScope.LET_POST -> {
                                val letDecls = if (itemVis.mslScope == MslScope.LET_POST) {
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
            }

            Namespace.TYPE -> {
                if (scope is MvTypeParametersOwner) {
                    if (processor.matchAll(itemVis, scope.typeParameters)) return true
                }
                if (scope is MvImportsOwner) {
                    if (processor.matchAll(itemVis, scope.itemImportNames())) return true
                }
                when (scope) {
                    is MvItemSpec -> {
                        val funcItem = scope.funcItem
                        if (funcItem != null) {
                            processor.matchAll(itemVis, funcItem.typeParameters)
                        } else {
                            false
                        }
                    }
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.matchAll(
                            itemVis,
                            scope.itemImportNames(),
                            module.structs()
                        )
                    }
                    is MvApplySchemaStmt -> {
                        val toPatterns = scope.applyTo?.functionPatternList.orEmpty()
                        val patternTypeParams = toPatterns.flatMap { it.typeParameters }
                        processor.matchAll(itemVis, patternTypeParams)
                    }

                    else -> false
                }
            }

            Namespace.SPEC_ITEM -> when (scope) {
                is MvModuleBlock -> {
                    val module = scope.parent as MvModule
                    processor.matchAll(
                        itemVis,
                        listOf(
                            module.allNonTestFunctions(),
                            module.structs(),
                        ).flatten()
                    )
                }
                else -> false
            }

            Namespace.SCHEMA -> when (scope) {
                is MvModuleBlock -> processor.matchAll(
                    itemVis,
                    scope.itemImportNames(),
                    scope.schemaList
                )
                is MvModuleSpecBlock -> processor.matchAll(
                    itemVis,
                    scope.itemImportNames(),
                    scope.schemaList,
                    scope.specFunctionList
                )
                else -> false
            }

            Namespace.MODULE -> when (scope) {
                is MvImportsOwner -> processor.matchAll(
                    itemVis,
                    listOf(
                        scope.moduleImportNames(),
                        scope.selfItemImports(),
                        scope.selfItemImportAliases(),
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
        if (handleScope(cameFrom, scope)) return true

        // walk all items in original module block
        if (scope is MvModuleBlock) {
            // handle spec module {}
            if (handleModuleItemSpecs(cameFrom, scope, handleScope)) return true
            // walk over all spec modules
            for (moduleSpec in scope.module.allModuleSpecs()) {
                val moduleSpecBlock = moduleSpec.moduleSpecBlock ?: continue
                if (handleScope(cameFrom, moduleSpecBlock)) return true
            }
        }

        if (scope is MvModuleSpecBlock) {
            val moduleBlock = scope.moduleSpec.module?.moduleBlock
            if (moduleBlock != null) {
                cameFrom = scope
                scope = moduleBlock
                continue
            }
        }

        if (stopAfter(scope)) break

        cameFrom = scope
        scope = scope.parent as? MvElement
    }

    return false
}

private fun handleModuleItemSpecs(
    cameFrom: MvElement,
    scope: MvElement,
    handleScope: (cameFrom: MvElement, scope: MvElement) -> Boolean
): Boolean {
    val moduleItemSpecs = when (scope) {
        is MvModuleBlock -> scope.moduleItemSpecList
        is MvModuleSpecBlock -> scope.moduleItemSpecList
        else -> emptyList()
    }
    for (moduleItemSpec in moduleItemSpecs.filter { it != cameFrom }) {
        val itemSpecBlock = moduleItemSpec.itemSpecBlock ?: continue
        if (handleScope(cameFrom, itemSpecBlock)) return true
    }
    return false
}
