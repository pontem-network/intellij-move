package org.move.lang.core.resolve

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.address
import org.move.lang.index.MvNamedElementIndex
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull
import org.move.stdext.wrapWithList

data class ItemVis(
    val namespaces: Set<Namespace>,
    val visibilities: Set<Visibility>,
    val mslLetScope: MslLetScope,
    val itemScope: ItemScope,
) {
    val isMsl get() = mslLetScope != MslLetScope.NONE
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

fun resolveLocalItem(
    element: MvReferenceElement,
    namespaces: Set<Namespace>
): List<MvNamedElement> {
    val itemVis = ItemVis(
        namespaces,
        mslLetScope = element.mslLetScope,
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

// go from local MODULE reference to corresponding FqModuleRef (from import)
fun resolveIntoFQModuleRefInUseSpeck(moduleRef: MvModuleRef): MvFQModuleRef? {
    check(moduleRef !is MvFQModuleRef) { "Should be handled on the upper level" }

    // module refers to ModuleImport
    var resolved = resolveLocalItem(moduleRef, setOf(Namespace.MODULE)).firstOrNull()
    if (resolved is MvUseAlias) {
        resolved = resolved.moduleUseSpeck ?: resolved.useItem
    }
    if (resolved is MvUseItem && resolved.isSelf) {
        return resolved.useSpeck().fqModuleRef
    }
//    if (resolved !is MvModuleUseSpeck) return null
    return (resolved as? MvModuleUseSpeck)?.fqModuleRef
//    return resolved.fqModuleRef
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

        item is MvFunction && Namespace.FUNCTION in itemVis.namespaces -> {
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
        if (
            Namespace.MODULE in itemVis.namespaces
            && processor.match(itemVis, module)
        ) {
            return true
        }
        if (processModuleInnerItems(module, itemVis, processor)) return true
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
        mslLetScope = fqModuleRef.mslLetScope,
        itemScope = fqModuleRef.itemScope,
    )
    val moveProj = fqModuleRef.moveProject ?: return
    val refAddressText = fqModuleRef.addressRef.address(moveProj)?.canonicalValue(moveProj)

    val moduleProcessor = MatchingProcessor<MvNamedElement> {
        val entry = SimpleScopeEntry(it.name, it.element as MvModule)
        val modAddressText = entry.element.address(moveProj)?.canonicalValue(moveProj)
        if (modAddressText != refAddressText)
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
    moduleRef: MvFQModuleRef,
    target: String,
    processor: MatchingProcessor<MvModule>,
) {
    val project = moduleRef.project
    val moveProj = moduleRef.moveProject ?: return
    val refAddress = moduleRef.addressRef.address(moveProj)?.canonicalValue(moveProj)

    val matchModule = MatchingProcessor<MvNamedElement> {
        val entry = SimpleScopeEntry(it.name, it.element as MvModule)
        // TODO: check belongs to the current project
        val modAddress = entry.element.address(moveProj)?.canonicalValue(moveProj)

        if (modAddress != refAddress) return@MatchingProcessor false
        processor.match(entry)
    }

    val itemVis = ItemVis(
        namespaces = setOf(Namespace.MODULE),
        visibilities = Visibility.local(),
        mslLetScope = moduleRef.mslLetScope,
        itemScope = moduleRef.itemScope,
    )
    // search modules in the current file first
    val currentFile = moduleRef.containingMoveFile ?: return
    val stopped = processFileItems(currentFile, itemVis, matchModule)
    if (stopped) return

    val currentFileScope = GlobalSearchScope.fileScope(currentFile)
    val searchScope =
        moveProj.searchScope().intersectWith(GlobalSearchScope.notScope(currentFileScope))

    MvNamedElementIndex
        .processElementsByName(project, target, searchScope) {
            val matched = processQualItem(it, itemVis, matchModule)
            if (matched) return@processElementsByName false

            true
        }
}

/// continue with the next resolution scope
const val CONTINUE = false
const val STOP = true

fun processLexicalDeclarations(
    scope: MvElement,
    cameFrom: MvElement,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (namespace in itemVis.namespaces) {
        val stop = when (namespace) {

            Namespace.STRUCT_FIELD -> {
                val structItem = when (scope) {
                    is MvStructPat -> scope.path.maybeStruct
                    is MvStructLitExpr -> scope.path.maybeStruct
                    else -> null
                }
                if (structItem != null) return processor.matchAll(itemVis, structItem.fields)
                false
            }

            Namespace.SCHEMA_FIELD -> {
                val schema = (scope as? MvSchemaLit)?.path?.maybeSchema
                if (schema != null) {
                    return processor.matchAll(itemVis, schema.fieldBindings)
                }
                false
            }

            Namespace.ERROR_CONST -> {
                val found = when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.matchAll(
                            itemVis,
                            module.consts(),
                        )
                    }
                    else -> false
                }
                if (!found) {
                    if (scope is MvImportsOwner) {
                        if (processor.matchAll(itemVis, scope.allUseItems())) return true
                    }
                }
                found
            }

            Namespace.NAME -> {
                val found = when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.matchAll(
                            itemVis,
                            module.structs(),
                            module.consts(),
                        )
                    }
                    is MvModuleSpecBlock -> processor.matchAll(itemVis, scope.schemaList)
                    is MvScript -> processor.matchAll(itemVis, scope.consts())
                    is MvFunctionLike -> processor.matchAll(itemVis, scope.allParamsAsBindings)
                    is MvLambdaExpr -> processor.matchAll(itemVis, scope.bindingPatList)
                    is MvItemSpec -> {
                        val item = scope.item
                        when (item) {
                            is MvFunction -> {
                                processor.matchAll(
                                    itemVis,
                                    item.valueParamsAsBindings,
                                    item.specResultParameters.map { it.bindingPat },
                                )
                            }
                            is MvStruct -> processor.matchAll(itemVis, item.fields)
                            else -> false
                        }
                    }
                    is MvSchema -> processor.matchAll(itemVis, scope.fieldBindings)
                    is MvQuantBindingsOwner -> processor.matchAll(itemVis, scope.bindings)
                    is MvCodeBlock,
                    is MvSpecCodeBlock -> {
                        val visibleLetStmts = when (scope) {
                            is MvCodeBlock -> {
                                scope.letStmts
                                    // drops all let-statements after the current position
                                    .filter { it.cameBefore(cameFrom) }
                                    // drops let-statement that is ancestors of ref (on the same statement, at most one)
                                    .filter {
                                        cameFrom != it
                                                && !PsiTreeUtil.isAncestor(cameFrom, it, true)
                                    }
                            }
                            is MvSpecCodeBlock -> {
                                when (itemVis.mslLetScope) {
                                    MslLetScope.EXPR_STMT -> scope.allLetStmts
                                    MslLetScope.LET_STMT, MslLetScope.LET_POST_STMT -> {
                                        val letDecls = if (itemVis.mslLetScope == MslLetScope.LET_POST_STMT) {
                                            scope.allLetStmts
                                        } else {
                                            scope.letStmts(false)
                                        }
                                        letDecls
                                            // drops all let-statements after the current position
                                            .filter { it.cameBefore(cameFrom) }
                                            // drops let-statement that is ancestors of ref (on the same statement, at most one)
                                            .filter {
                                                cameFrom != it
                                                        && !PsiTreeUtil.isAncestor(cameFrom, it, true)
                                            }
                                    }
                                    MslLetScope.NONE -> emptyList()
                                }
                            }
                            else -> error("unreachable")
                        }
                        // shadowing support (look at latest first)
                        val namedElements = visibleLetStmts
                            .asReversed()
                            .flatMap { it.pat?.bindings.orEmpty() }

                        // skip shadowed (already visited) elements
                        val visited = mutableSetOf<String>()
                        val processorWithShadowing = MatchingProcessor { entry ->
                            ((entry.name !in visited)
                                    && processor.match(entry).also { visited += entry.name })
                        }
                        var found = processorWithShadowing.matchAll(itemVis, namedElements)
                        if (!found && scope is MvSpecCodeBlock) {
                            // if inside SpecCodeBlock, match also with builtin spec consts and global variables
                            found = processorWithShadowing.matchAll(
                                itemVis,
                                scope.builtinSpecConsts(),
                                scope.globalVariables()
                            )
                        }
                        found
                    }
                    else -> false
                }
                if (!found) {
                    if (scope is MvImportsOwner) {
                        if (processor.matchAll(itemVis, scope.allUseItems())) return true
                    }
                }
                found
            }
            Namespace.FUNCTION -> {
                val found = when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        val specFunctions = if (itemVis.isMsl) {
                            listOf(module.specFunctions(), module.builtinSpecFunctions()).flatten()
                        } else {
                            emptyList()
                        }
                        val specInlineFunctions = if (itemVis.isMsl) {
                            module.moduleItemSpecs().flatMap { it.specInlineFunctions() }
                        } else {
                            emptyList()
                        }
                        processor.matchAll(
                            itemVis,
                            module.allNonTestFunctions(),
                            module.builtinFunctions(),
                            specFunctions,
                            specInlineFunctions
                        )
                    }
                    is MvModuleSpecBlock -> {
                        val specFunctions = scope.specFunctionList
                        val specInlineFunctions = scope.moduleItemSpecList.flatMap { it.specInlineFunctions() }
                        processor.matchAll(
                            itemVis,
                            specFunctions,
                            specInlineFunctions
                        )
                    }
                    is MvFunctionLike -> processor.matchAll(itemVis, scope.lambdaParamsAsBindings)
                    is MvLambdaExpr -> processor.matchAll(itemVis, scope.bindingPatList)
                    is MvItemSpec -> {
                        val item = scope.item
                        when (item) {
                            is MvFunction -> processor.matchAll(itemVis, item.lambdaParamsAsBindings)
                            else -> false
                        }
                    }
                    is MvSpecCodeBlock -> {
                        val inlineFunctions = scope.specInlineFunctions().asReversed()
                        return processor.matchAll(itemVis, inlineFunctions)
                    }
                    else -> false
                }
                if (!found) {
                    if (scope is MvImportsOwner) {
                        if (processor.matchAll(itemVis, scope.allUseItems())) return true
                    }
                }
                found
            }

            Namespace.TYPE -> {
                if (scope is MvTypeParametersOwner) {
                    if (processor.matchAll(itemVis, scope.typeParameters)) return true
                }
                val found = when (scope) {
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
                            scope.allUseItems(),
                            module.structs()
                        )
                    }
                    is MvApplySchemaStmt -> {
                        val toPatterns = scope.applyTo?.functionPatternList.orEmpty()
                        val patternTypeParams =
                            toPatterns.flatMap { it.typeParameterList?.typeParameterList.orEmpty() }
                        processor.matchAll(itemVis, patternTypeParams)
                    }

                    else -> false
                }
                if (!found) {
                    if (scope is MvImportsOwner) {
                        if (processor.matchAll(itemVis, scope.allUseItems())) return true
                    }
                }
                found
            }

            Namespace.SCHEMA -> when (scope) {
                is MvModuleBlock -> processor.matchAll(
                    itemVis,
                    scope.allUseItems(),
                    scope.schemaList
                )
                is MvModuleSpecBlock -> processor.matchAll(
                    itemVis,
                    scope.allUseItems(),
                    scope.schemaList,
                    scope.specFunctionList
                )
                else -> false
            }

            Namespace.MODULE -> when (scope) {
                is MvImportsOwner ->
                    processor.matchAll(itemVis, scope.moduleUseItems())
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
    var scope = start.context as MvElement?
    while (scope != null) {
        if (handleScope(cameFrom, scope)) return true

        // walk all items in original module block
        if (scope is MvModuleBlock) {
            // handle spec module {}
            if (handleModuleItemSpecsInBlock(cameFrom, scope, handleScope)) return true
            // walk over all spec modules
            for (moduleSpec in scope.module.allModuleSpecs()) {
                val moduleSpecBlock = moduleSpec.moduleSpecBlock ?: continue
                if (handleScope(cameFrom, moduleSpecBlock)) return true
                if (handleModuleItemSpecsInBlock(cameFrom, moduleSpecBlock, handleScope)) return true
            }
        }

        if (scope is MvModuleSpecBlock) {
            val moduleBlock = scope.moduleSpec.moduleItem?.moduleBlock
            if (moduleBlock != null) {
                cameFrom = scope
                scope = moduleBlock
                continue
            }
        }

        if (stopAfter(scope)) break

        cameFrom = scope
        scope = scope.context as? MvElement
    }

    return false
}

private fun handleModuleItemSpecsInBlock(
    cameFrom: MvElement,
    block: MvElement,
    handleScope: (cameFrom: MvElement, scope: MvElement) -> Boolean
): Boolean {
    val moduleItemSpecs = when (block) {
        is MvModuleBlock -> block.moduleItemSpecList
        is MvModuleSpecBlock -> block.moduleItemSpecList
        else -> emptyList()
    }
    for (moduleItemSpec in moduleItemSpecs.filter { it != cameFrom }) {
        val itemSpecBlock = moduleItemSpec.itemSpecBlock ?: continue
        if (handleScope(cameFrom, itemSpecBlock)) return true
    }
    return false
}
