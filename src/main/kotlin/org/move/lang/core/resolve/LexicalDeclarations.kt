package org.move.lang.core.resolve

import com.intellij.psi.util.CachedValueProvider
import com.intellij.util.containers.addIfNotNull
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.scopeEntry.asEntries
import org.move.lang.core.resolve.scopeEntry.asEntry
import org.move.lang.core.resolve.scopeEntry.useSpeckEntries
import org.move.stdext.chain
import org.move.utils.psiCacheResult

fun getEntriesInScope(scope: MvElement, cameFrom: MvElement): List<ScopeEntry> {
    return buildList {
        if (scope is MvGenericDeclaration) {
            addAll(scope.typeParameters.asEntries())
        }
        when (scope) {
            is MvModule -> {
                addAll(ModuleResolveScope(scope).getResults())
            }
            is MvScript -> {
                addAll(scope.constList.asEntries())
            }
            is MvFunctionLike -> {
                addAll(scope.parametersAsBindings.asEntries())
            }
            is MvLambdaExpr -> {
                addAll(scope.lambdaParametersAsBindings.asEntries())
            }
            is MvItemSpec -> {
                addAll(ItemSpecResolveScope(scope).getResults())
            }

            is MvSchema -> {
                addAll(scope.fieldsAsBindings.asEntries())
            }

            is MvModuleSpecBlock -> {
                addAll(ModuleSpecBlockResolveScope(scope).getResults())
            }

            is MvCodeBlock -> {
                val (letBindings, _) = getVisibleLetPatBindingsWithShadowing(scope, cameFrom)
                addAll(letBindings)
            }

            is MvSpecCodeBlock -> {
                val (letBindings, visited) = getVisibleLetPatBindingsWithShadowing(scope, cameFrom)
                addAll(letBindings)

                val globalEntries = scope.builtinSpecConsts().asEntries()
                    .chain(scope.globalVariables().asEntries())
                for (scopeEntry in globalEntries) {
                    if (scopeEntry.name in visited) continue
                    visited += scopeEntry.name
                    add(scopeEntry)
                }

                val inlineFunctions = scope.specInlineFunctions().asReversed().asEntries()
                addAll(inlineFunctions)
            }

            is MvQuantBindingsOwner -> {
                addAll(scope.bindings.asEntries())
            }

            is MvForExpr -> {
                val iterBinding = scope.forIterCondition?.patBinding
                if (iterBinding != null) {
                    addIfNotNull(iterBinding.asEntry())
                }
            }
            is MvMatchArm -> {
                if (cameFrom !is MvPat) {
                    // coming from rhs, use pat bindings from lhs
                    addAll(scope.pat.bindings.asEntries())
                }
            }

            is MvApplySchemaStmt -> {
                val toPatterns = scope.applyTo?.functionPatternList.orEmpty()
                val patternTypeParams =
                    toPatterns.flatMap { it.typeParameterList?.typeParameterList.orEmpty() }
                addAll(patternTypeParams.asEntries())
            }
        }

        if (scope is MvItemsOwner) {
            addAll(scope.useSpeckEntries)
        }
    }
}

private fun getVisibleLetPatBindingsWithShadowing(
    scope: MvElement,
    stmtOrTailExpr: MvElement,
): Pair<List<ScopeEntry>, MutableSet<String>> {
    val visibleLetStmts = when (scope) {
        is MvCodeBlock -> {
            BlockLetStmts(scope).getResults()
                // drops all let-statements after the current position
                .filter { it.first.strictlyBefore(stmtOrTailExpr) }
        }
        is MvSpecCodeBlock -> {
            val currentLetStmt = stmtOrTailExpr as? MvLetStmt
            val allLetStmts = BlockLetStmts(scope).getResults()
            when {
                currentLetStmt != null -> {
                    // if post = true, then both pre and post are accessible, else only pre
                    val letStmts = if (currentLetStmt.post) {
                        allLetStmts
                    } else {
                        allLetStmts.filter { !it.first.post }
                    }
                    letStmts
                        // drops all let-statements after the current position
                        .filter { it.first.strictlyBefore(stmtOrTailExpr) }
                }
                else -> allLetStmts
            }
        }
        else -> error("unreachable")
    }
    // shadowing support (look at latest first)
    val bindingEntries = visibleLetStmts.asReversed().flatMap { it.second }

    val visited = mutableSetOf<String>()
    val bindings = buildList {
        for (binding in bindingEntries) {
            val name = binding.name
            if (name in visited) continue
            visited += name
            add(binding)
        }
    }
    return bindings to visited
}

class BlockLetStmts(override val owner: AnyBlock): PsiCachedValueProvider<List<Pair<MvLetStmt, List<ScopeEntry>>>> {
    override fun compute(): CachedValueProvider.Result<List<Pair<MvLetStmt, List<ScopeEntry>>>> {
        val letStmts = owner.stmtList.filterIsInstance<MvLetStmt>()
            .map {
                val bindings = it.pat?.bindings.orEmpty().asEntries()
                it to bindings
            }
        return owner.psiCacheResult(letStmts)
    }
}
