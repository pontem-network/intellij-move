package org.move.lang.core.resolve

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.inspections.imports.usageScope
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.resolve.util.forEachLeafSpeck
import org.move.lang.core.resolve2.itemScopeEntries
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
    if (scope is MvGenericDeclaration) {
        if (processor.processAll(scope.typeParameters.mapNotNull { it.asEntry() })) return true
    }
    when (scope) {
        is MvModule -> {
            if (processor.processAll(scope.itemScopeEntries)) return true

            if (processor.processAll(scope.enumVariants().mapNotNull { it.asEntry() })) return true
            if (processor.processAll(
                    scope.builtinFunctions().mapNotNull { it.asEntry() },
                    scope.builtinSpecFunctions().mapNotNull { it.asEntry() },
                )
            ) return true
        }
        is MvScript -> {
            if (processor.processAll(scope.constList.mapNotNull { it.asEntry() })) return true
        }
        is MvFunctionLike -> {
            if (processor.processAll(scope.parametersAsBindings.map { it.asEntry() })) return true
        }
        is MvLambdaExpr -> {
            if (processor.processAll(scope.lambdaParametersAsBindings.map { it.asEntry() })) return true
        }
        is MvItemSpec -> {
            val refItem = scope.item
            when (refItem) {
                is MvFunction -> {
                    if (processor.processAll(refItem.typeParameters.mapNotNull { it.asEntry() })) return true

                    if (processor.processAll(
                            refItem.parametersAsBindings.map { it.asEntry() },
                            refItem.specFunctionResultParameters.map { it.patBinding }.map { it.asEntry() }
                        )
                    ) {
                        return true
                    }
                }
                is MvStruct -> {
                    if (processor.processAll(refItem.namedFields.mapNotNull { it.asEntry() })) return true
                }
            }
        }

        is MvSchema -> {
            if (processor.processAll(scope.fieldsAsBindings.map { it.asEntry() })) return true
        }

        is MvModuleSpecBlock -> {
            val specFuns = scope.specFunctionList
            val specInlineFuns = scope.moduleItemSpecList.flatMap { it.specInlineFunctions() }
            if (processor.processAll(
                    specFuns.mapNotNull { it.asEntry() },
                    specInlineFuns.mapNotNull { it.asEntry() })
            ) return true

            if (processor.processAll(
                    scope.schemaList.mapNotNull { it.asEntry() },
                    specFuns.mapNotNull { it.asEntry()?.copyWithNs(SCHEMAS) }
                )
            ) return true
        }

        is MvCodeBlock -> {
            val letBindings = getVisibleLetPatBindings(scope, cameFrom, ctx)
            // skip shadowed (already visited) elements
            val visited = mutableSetOf<String>()
            val variablesProcessor = processor.wrapWithFilter {
                val isVisited = it.name in visited
                if (!isVisited) {
                    visited += it.name
                }
                !isVisited
            }
            if (variablesProcessor.processAll(letBindings.map { it.asEntry() })) return true
        }

        is MvSpecCodeBlock -> {
            val letBindings = getVisibleLetPatBindings(scope, cameFrom, ctx)
            // skip shadowed (already visited) elements
            val visited = mutableSetOf<String>()
            val variablesProcessor = processor.wrapWithFilter {
                val isVisited = it.name in visited
                if (!isVisited) {
                    visited += it.name
                }
                !isVisited
            }
            if (variablesProcessor.processAll(letBindings.map { it.asEntry() })) return true

            // if inside SpecCodeBlock, process also with builtin spec consts and global variables
            if (variablesProcessor.processAll(
                    scope.builtinSpecConsts().mapNotNull { it.asEntry() },
                    scope.globalVariables().mapNotNull { it.asEntry() }
                )
            ) return true

            val inlineFunctions = scope.specInlineFunctions().asReversed().mapNotNull { it.asEntry() }
            if (processor.processAll(inlineFunctions)) return true
        }

        is MvQuantBindingsOwner -> {
            if (processor.processAll(scope.bindings.map { it.asEntry() })) return true
        }

        is MvForExpr -> {
            val iterBinding = scope.forIterCondition?.patBinding
            val stop = if (iterBinding != null) {
                processor.process(iterBinding.asEntry())
            } else {
                false
            }
            if (stop) return true
        }
        is MvMatchArm -> {
            if (cameFrom !is MvPat) {
                // coming from rhs, use pat bindings from lhs
                if (processor.processAll(scope.pat.bindings.map { it.asEntry() })) return true
            }
        }

        is MvApplySchemaStmt -> {
            val toPatterns = scope.applyTo?.functionPatternList.orEmpty()
            val patternTypeParams =
                toPatterns.flatMap { it.typeParameterList?.typeParameterList.orEmpty() }
            if (processor.processAll(patternTypeParams.mapNotNull { it.asEntry() })) return true
        }
    }

    if (scope is MvItemsOwner) {
        if (scope.processUseSpeckElements(ns, processor)) return true
    }

    return false
}

private fun getVisibleLetPatBindings(
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
            val currentLetStmt = ctx.element.ancestorOrSelf<MvLetStmt>(stopAt = MvSpecCodeBlock::class.java)
            when {
                currentLetStmt != null -> {
                    // if post = true, then both pre and post are accessible, else only pre
                    val letStmts = if (currentLetStmt.post) {
                        scope.allLetStmts
                    } else {
                        scope.allLetStmts.filter { !it.post }
                    }
                    letStmts
                        // drops all let-statements after the current position
                        .filter { it.cameBefore(cameFrom) }
                        // drops let-statement that is ancestors of ref (on the same statement, at most one)
                        .filter {
                            cameFrom != it
                                    && !PsiTreeUtil.isAncestor(cameFrom, it, true)
                        }

                }
                else -> scope.allLetStmts
            }
        }
        else -> error("unreachable")
    }
    // shadowing support (look at latest first)
    val letPatBindings = visibleLetStmts
        .asReversed()
        .flatMap { it.pat?.bindings.orEmpty() }
    return letPatBindings
}

private fun MvItemsOwner.processUseSpeckElements(ns: Set<Namespace>, processor: RsResolveProcessor): Boolean {
    val useSpeckItems = getUseSpeckItems()
    for (useSpeckItem in useSpeckItems) {
        val speckPath = useSpeckItem.speckPath
        val alias = useSpeckItem.alias

        val resolvedItem = speckPath.reference?.resolve()
        if (resolvedItem == null) {
            // aliased element cannot be resolved, but alias itself is valid, resolve to it
            if (alias != null) {
                val referenceName = useSpeckItem.aliasOrSpeckName ?: continue
                if (processor.process(SimpleScopeEntry(
                        referenceName,
                        alias,
                        ALL_NAMESPACES
                    ))) return true
//                if (processor.processNamedElement(referenceName, ALL_NAMESPACES, alias)) return true
            }
            continue
        }

        val element = alias ?: resolvedItem
        val namespace = resolvedItem.moduleItemNamespace
        if (namespace in ns) {
            val speckItemName = useSpeckItem.aliasOrSpeckName ?: continue
            if (processor.process(
                    ScopeEntryWithVisibility(
                        speckItemName,
                        element,
                        ns,
                        adjustedItemScope = useSpeckItem.stmtUsageScope
                    )
                )
            ) return true
        }
    }
    return false
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
    val aliasOrSpeckName: String?
        get() {
            if (alias != null) {
                return alias.name
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