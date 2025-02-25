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
import org.move.stdext.chain
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.psiCacheResult

fun getEntriesInScope(
    scope: MvElement,
    cameFrom: MvElement,
    ctx: ResolutionContext,
): List<ScopeEntry> {
    return buildList {
        if (scope is MvGenericDeclaration) {
            addAll(scope.typeParameters.mapNotNull { it.asEntry() })
        }
        when (scope) {
            is MvModule -> {
                addAll(scope.itemScopeEntries)

                addAll(scope.enumVariants().mapNotNull { it.asEntry() })
                addAll(scope.builtinFunctions().mapNotNull { it.asEntry() })
                addAll(scope.builtinSpecFunctions().mapNotNull { it.asEntry() })
            }
            is MvScript -> {
                addAll(scope.constList.mapNotNull { it.asEntry() })
            }
            is MvFunctionLike -> {
                addAll(scope.parametersAsBindings.map { it.asEntry() })
            }
            is MvLambdaExpr -> {
                addAll(scope.lambdaParametersAsBindings.map { it.asEntry() })
            }
            is MvItemSpec -> {
                val refItem = scope.item
                when (refItem) {
                    is MvFunction -> {
                        addAll(refItem.typeParameters.mapNotNull { it.asEntry() })

                        addAll(refItem.parametersAsBindings.map { it.asEntry() })
                        addAll(refItem.specFunctionResultParameters.map { it.patBinding }.map { it.asEntry() })
                    }
                    is MvStruct -> {
                        addAll(refItem.namedFields.mapNotNull { it.asEntry() })
                    }
                }
            }

            is MvSchema -> {
                addAll(scope.fieldsAsBindings.map { it.asEntry() })
            }

            is MvModuleSpecBlock -> {
                val specFuns = scope.specFunctionList
                addAll(specFuns.mapNotNull { it.asEntry() })

                val specInlineFuns = scope.moduleItemSpecList.flatMap { it.specInlineFunctions() }
                addAll(specInlineFuns.mapNotNull { it.asEntry() })

                addAll(scope.schemaList.mapNotNull { it.asEntry() })
                addAll(specFuns.mapNotNull { it.asEntry()?.copyWithNs(SCHEMAS) })
            }

            is MvCodeBlock -> {
                val (letBindings, _) = getVisibleLetPatBindingsWithShadowing(scope, cameFrom, ctx)
                addAll(letBindings.map { it.asEntry() })
            }

            is MvSpecCodeBlock -> {
                val (letBindings, visited) = getVisibleLetPatBindingsWithShadowing(scope, cameFrom, ctx)
                addAll(letBindings.map { it.asEntry() })

                val globalEntries = scope.builtinSpecConsts().mapNotNull { it.asEntry() }
                    .chain(scope.globalVariables().mapNotNull { it.asEntry() })
                for (scopeEntry in globalEntries) {
                    if (scopeEntry.name in visited) continue
                    visited += scopeEntry.name
                    add(scopeEntry)
                }

                val inlineFunctions = scope.specInlineFunctions().asReversed().mapNotNull { it.asEntry() }
                addAll(inlineFunctions)
            }

            is MvQuantBindingsOwner -> {
                addAll(scope.bindings.map { it.asEntry() })
            }

            is MvForExpr -> {
                val iterBinding = scope.forIterCondition?.patBinding
                if (iterBinding != null) {
                    add(iterBinding.asEntry())
                }
            }
            is MvMatchArm -> {
                if (cameFrom !is MvPat) {
                    // coming from rhs, use pat bindings from lhs
                    addAll(scope.pat.bindings.map { it.asEntry() })
                }
            }

            is MvApplySchemaStmt -> {
                val toPatterns = scope.applyTo?.functionPatternList.orEmpty()
                val patternTypeParams =
                    toPatterns.flatMap { it.typeParameterList?.typeParameterList.orEmpty() }
                addAll(patternTypeParams.mapNotNull { it.asEntry() })
            }
        }

        if (scope is MvItemsOwner) {
            addAll(scope.getUseSpeckElements())
        }
    }
}

private fun getVisibleLetPatBindingsWithShadowing(
    scope: MvElement,
    cameFrom: MvElement,
    ctx: ResolutionContext,
): Pair<List<MvPatBinding>, MutableSet<String>> {
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

    val visited = mutableSetOf<String>()
    val bindings = buildList {
        for (binding in letPatBindings) {
            val name = binding.name
            if (name in visited) continue
            visited += name
            add(binding)
        }
    }
    return bindings to visited
}

private fun MvItemsOwner.getUseSpeckElements(): List<ScopeEntry> {
    val useSpeckItems = getUseSpeckItems()

    return buildList {
        for (useSpeckItem in useSpeckItems) {
            val speckPath = useSpeckItem.speckPath
            val alias = useSpeckItem.alias

            val resolvedItem = speckPath.reference?.resolve()
            if (resolvedItem == null) {
                // aliased element cannot be resolved, but alias itself is valid, resolve to it
                if (alias != null) {
                    val referenceName = useSpeckItem.aliasOrSpeckName ?: continue
                    add(
                        SimpleScopeEntry(
                            referenceName,
                            alias,
                            ALL_NAMESPACES
                        )
                    )
                }
                continue
            }

            val element = alias ?: resolvedItem
            val itemNs = resolvedItem.itemNs
            val speckItemName = useSpeckItem.aliasOrSpeckName ?: continue
            add(
                ScopeEntryWithVisibility(
                    speckItemName,
                    element,
                    itemNs,
                    adjustedItemScope = useSpeckItem.stmtUsageScope
                )
            )
//            if (namespace in ns) {
//            }
        }
    }
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