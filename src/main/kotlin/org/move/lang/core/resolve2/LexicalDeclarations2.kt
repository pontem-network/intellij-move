package org.move.lang.core.resolve2

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.inspections.imports.usageScope
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.LetStmtScope.*
import org.move.lang.core.resolve.ref.ENUMS
import org.move.lang.core.resolve.ref.NONE
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.TYPES
import org.move.lang.core.resolve2.ref.ResolutionContext
import org.move.lang.core.resolve2.util.forEachLeafSpeck
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.psiCacheResult

fun processItemsInScope(
    scope: MvElement,
    cameFrom: MvElement,
    ns: Set<Namespace>,
    ctx: ResolutionContext,
    processor: RsResolveProcessor,
): Boolean {
    for (namespace in ns) {
        val elementNs = setOf(namespace)
        val stop = when (namespace) {
            Namespace.NAME -> {
                val found = when (scope) {
                    is MvModule -> {
                        // try enum variants first
                        if (processor.processAll(elementNs, scope.enumVariants())) {
                            return true
                        }
                        processor.processAllItems(elementNs, scope.constList)
                    }
                    is MvScript -> processor.processAllItems(elementNs, scope.constList)
                    is MvFunctionLike -> processor.processAll(elementNs, scope.parametersAsBindings)
                    is MvLambdaExpr -> processor.processAll(elementNs, scope.lambdaParametersAsBindings)
                    is MvForExpr -> {
                        val iterBinding = scope.forIterCondition?.patBinding
                        if (iterBinding != null) {
                            processor.process(elementNs, iterBinding)
                        } else {
                            false
                        }
                    }
                    is MvMatchArm -> {
                        if (cameFrom !is MvPat) {
                            // coming from rhs, use pat bindings from lhs
                            if (processor.processAll(elementNs, scope.pat.bindings)) return true
                            continue
                        }

                        false
                    }
                    is MvItemSpec -> {
                        val referredItem = scope.item
                        when (referredItem) {
                            is MvFunction -> {
                                processor.processAll(
                                    elementNs,
                                    referredItem.valueParamsAsBindings,
                                    referredItem.specFunctionResultParameters.map { it.patBinding },
                                )
                            }
                            is MvStruct -> processor.processAll(elementNs, referredItem.namedFields)
                            else -> false
                        }
                    }
                    is MvSchema -> processor.processAll(elementNs, scope.fieldsAsBindings)
                    is MvQuantBindingsOwner -> processor.processAll(elementNs, scope.bindings)
                    is MvCodeBlock -> {
                        val letBindings = getVisibleLetBindings(scope, cameFrom, ctx)
                        // skip shadowed (already visited) elements
                        val visited = mutableSetOf<String>()
                        val variablesProcessor = processor.wrapWithFilter {
                            val isVisited = it.name in visited
                            if (!isVisited) {
                                visited += it.name
                            }
                            !isVisited
                        }
                        variablesProcessor.processAll(elementNs, letBindings)
                    }
                    is MvSpecCodeBlock -> {
                        val letBindings = getVisibleLetBindings(scope, cameFrom, ctx)
                        // skip shadowed (already visited) elements
                        val visited = mutableSetOf<String>()
                        val variablesProcessor = processor.wrapWithFilter {
                            val isVisited = it.name in visited
                            if (!isVisited) {
                                visited += it.name
                            }
                            !isVisited
                        }
                        var found = variablesProcessor.processAll(elementNs, letBindings)
                        if (!found) {
                            // if inside SpecCodeBlock, process also with builtin spec consts and global variables
                            found = variablesProcessor.processAllItems(
                                ns,
                                scope.builtinSpecConsts(),
                                scope.globalVariables()
                            )
                        }
                        found
                    }
                    else -> false
                }
                found
            }
            Namespace.FUNCTION -> {
                val found = when (scope) {
                    is MvModule -> {
                        val specFunctions =
                            listOf(scope.specFunctions(), scope.builtinSpecFunctions()).flatten()
                        val specInlineFunctions = scope.moduleItemSpecList.flatMap { it.specInlineFunctions() }
                        processor.processAllItems(
                            ns,
                            scope.tupleStructs(),
                            scope.builtinFunctions(),
                            scope.allNonTestFunctions(),
                            specFunctions,
                            specInlineFunctions
                        )
                    }
                    is MvModuleSpecBlock -> {
                        val specFunctions = scope.specFunctionList
                        val specInlineFunctions = scope.moduleItemSpecList.flatMap { it.specInlineFunctions() }
                        processor.processAllItems(
                            ns,
                            specFunctions,
                            specInlineFunctions
                        )
                    }
                    is MvFunctionLike -> processor.processAll(elementNs, scope.lambdaParamsAsBindings)
                    is MvLambdaExpr -> processor.processAll(elementNs, scope.lambdaParametersAsBindings)
                    is MvItemSpec -> {
                        val item = scope.item
                        when (item) {
                            is MvFunction -> processor.processAll(elementNs, item.lambdaParamsAsBindings)
                            else -> false
                        }
                    }
                    is MvSpecCodeBlock -> {
                        val inlineFunctions = scope.specInlineFunctions().asReversed()
                        processor.processAllItems(ns, inlineFunctions)
                    }
                    else -> false
                }
                found
            }
            Namespace.TYPE -> {
                if (scope is MvGenericDeclaration) {
                    if (processor.processAll(elementNs, scope.typeParameters)) return true
                }
                val found = when (scope) {
                    is MvModule -> {
                        if (processor.processAll(TYPES, scope.enumVariants())) return true
                        processor.processAllItems(
                            TYPES,
                            scope.structs(),
                        )
                    }
                    is MvItemSpec -> {
                        val funcItem = scope.funcItem
                        if (funcItem != null) {
                            processor.processAll(elementNs, funcItem.typeParameters)
                        } else {
                            false
                        }
                    }
                    is MvApplySchemaStmt -> {
                        val toPatterns = scope.applyTo?.functionPatternList.orEmpty()
                        val patternTypeParams =
                            toPatterns.flatMap { it.typeParameterList?.typeParameterList.orEmpty() }
                        processor.processAll(elementNs, patternTypeParams)
                    }
                    else -> false
                }
                found
            }
            Namespace.ENUM -> {
                if (scope is MvModule) {
                    if (processor.processAllItems(ENUMS, scope.enumList)) return true
                }
                false
            }
            Namespace.SCHEMA -> when (scope) {
                is MvModule -> processor.processAllItems(ns, scope.schemaList)
                is MvModuleSpecBlock -> processor.processAllItems(ns, scope.schemaList, scope.specFunctionList)
                else -> false
            }
            else -> false
        }
        if (stop) return true
    }

    if (scope is MvItemsOwner) {
        if (scope.processUseSpeckElements(ns, processor)) return true
    }

    return false
}

private fun getVisibleLetBindings(
    scope: MvElement,
    cameFrom: MvElement,
    ctx: ResolutionContext,
): List<MvPatBinding> {
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
            val letStmtScope = ctx.element.letStmtScope
            when (letStmtScope) {
                EXPR_STMT -> scope.allLetStmts
                LET_STMT, LET_POST_STMT -> {
                    val letDecls =
                        if (letStmtScope == LET_POST_STMT) {
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
                NOT_MSL -> emptyList()
            }
        }
        else -> error("unreachable")
    }
    // shadowing support (look at latest first)
    val letBindings = visibleLetStmts
        .asReversed()
        .flatMap { it.pat?.bindings.orEmpty() }
    return letBindings
}

private fun MvItemsOwner.processUseSpeckElements(ns: Set<Namespace>, processor: RsResolveProcessor): Boolean {
    val useSpeckItems = getUseSpeckItems()
    for (useSpeckItem in useSpeckItems) {
        val speckPath = useSpeckItem.speckPath
        val alias = useSpeckItem.alias

        val resolvedItem = speckPath.reference?.resolve()
        if (resolvedItem == null) {
            if (alias != null) {
                val referenceName = useSpeckItem.referenceName ?: continue
                // aliased element cannot be resolved, but alias itself is valid, resolve to it
                if (processor.process(referenceName, NONE, alias)) return true
            }
            continue
        }

        val element = alias ?: resolvedItem
        val namespace = resolvedItem.namespace
        if (namespace in ns) {
            val referenceName = useSpeckItem.referenceName ?: continue
            if (processor.process(
                    referenceName,
                    element,
                    ns,
                    adjustedItemScope = useSpeckItem.stmtUsageScope
                )
            ) return true
        }
    }
    return false
//    var stop = false
//    for (useStmt in this.useStmtList) {
//        val stmtUsageScope = useStmt.usageScope
//        useStmt.forEachLeafSpeck { speckPath, alias ->
//            val name = if (alias != null) {
//                alias.name ?: return@forEachLeafSpeck false
//            } else {
//                var n = speckPath.referenceName ?: return@forEachLeafSpeck false
//                // 0x1::m::Self -> 0x1::m
//                if (n == "Self") {
//                    n = speckPath.qualifier?.referenceName ?: return@forEachLeafSpeck false
//                }
//                n
//            }
//            val resolvedItem = speckPath.reference?.resolve()
//            if (resolvedItem == null) {
//                if (alias != null) {
//                    // aliased element cannot be resolved, but alias itself is valid, resolve to it
//                    if (processor.process(name, NONE, alias)) {
//                        stop = true
//                        return@forEachLeafSpeck true
//                    }
//                }
//                return@forEachLeafSpeck false
//            }
//
//            val element = alias ?: resolvedItem
//            val namespace = resolvedItem.namespace
//            if (namespace in ns) {
//                val visibilityFilter =
//                    resolvedItem.visInfo(adjustScope = stmtUsageScope).createFilter()
//                if (processor.process(name, element, ns, visibilityFilter)) {
//                    stop = true
//                    return@forEachLeafSpeck true
//                }
//            }
//            false
//        }
//        if (stop) return true
//    }
//    return stop
}

private val USE_SPECK_ITEMS_KEY: Key<CachedValue<List<UseSpeckItem>>> = Key.create("USE_SPECK_ITEMS_KEY")

private fun MvItemsOwner.getUseSpeckItems(): List<UseSpeckItem> =
    project.cacheManager.cache(this, USE_SPECK_ITEMS_KEY) {
        val items = buildList {
            for (useStmt in useStmtList) {
                val usageScope = useStmt.usageScope
                useStmt.forEachLeafSpeck { speckPath, alias ->
                    add(UseSpeckItem(speckPath, alias, usageScope))
                }
            }
        }
        this.psiCacheResult(items)
    }

private data class UseSpeckItem(
    val speckPath: MvPath,
    val alias: MvUseAlias?,
    val stmtUsageScope: NamedItemScope
) {
    val referenceName: String?
        get() {
            if (alias != null) {
                return alias.name
//            alias.name ?: return null
            } else {
                var n = speckPath.referenceName ?: return null
                // 0x1::m::Self -> 0x1::m
                if (n == "Self") {
                    n = speckPath.qualifier?.referenceName ?: return null
                }
                return n
            }
        }
}

//private fun MvItemsOwner.processUseSpeckElements(ns: Set<Namespace>, processor: RsResolveProcessor): Boolean {
//    var stop = false
//    for (useStmt in this.useStmtList) {
//        val stmtUsageScope = useStmt.usageScope
//        useStmt.forEachLeafSpeck { speckPath, alias ->
//            val name = if (alias != null) {
//                alias.name ?: return@forEachLeafSpeck false
//            } else {
//                var n = speckPath.referenceName ?: return@forEachLeafSpeck false
//                // 0x1::m::Self -> 0x1::m
//                if (n == "Self") {
//                    n = speckPath.qualifier?.referenceName ?: return@forEachLeafSpeck false
//                }
//                n
//            }
//            val resolvedItem = speckPath.reference?.resolve()
//            if (resolvedItem == null) {
//                if (alias != null) {
//                    // aliased element cannot be resolved, but alias itself is valid, resolve to it
//                    if (processor.process(name, NONE, alias)) {
//                        stop = true
//                        return@forEachLeafSpeck true
//                    }
//                }
//                return@forEachLeafSpeck false
//            }
//
//            val element = alias ?: resolvedItem
//            val namespace = resolvedItem.namespace
//            if (namespace in ns) {
//                val visibilityFilter =
//                    resolvedItem.visInfo(adjustScope = stmtUsageScope).createFilter()
//                if (processor.process(name, element, ns, visibilityFilter)) {
//                    stop = true
//                    return@forEachLeafSpeck true
//                }
//            }
//            false
//        }
//        if (stop) return true
//    }
//    return stop
//}
