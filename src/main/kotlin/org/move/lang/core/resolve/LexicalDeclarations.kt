package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.addIfNotNull
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.scopeEntry.asEntries
import org.move.lang.core.resolve.scopeEntry.asEntry
import org.move.lang.core.resolve.scopeEntry.itemEntries
import org.move.lang.core.resolve.scopeEntry.useSpeckEntries
import org.move.lang.core.resolve.ref.ResolutionContext
import org.move.stdext.chain

fun getEntriesInScope(
    scope: MvElement,
    cameFrom: MvElement,
    ctx: ResolutionContext,
): List<ScopeEntry> {
    return buildList {
        if (scope is MvGenericDeclaration) {
            addAll(scope.typeParameters.asEntries())
        }
        when (scope) {
            is MvModule -> {
                addAll(scope.itemEntries)

                addAll(scope.enumVariants().asEntries())
                addAll(scope.builtinFunctions().asEntries())
                addAll(scope.builtinSpecFunctions().asEntries())
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
                val refItem = scope.item
                when (refItem) {
                    is MvFunction -> {
                        addAll(refItem.typeParameters.asEntries())

                        addAll(refItem.parametersAsBindings.asEntries())
                        addAll(refItem.specFunctionResultParameters.map { it.patBinding }.asEntries())
                    }
                    is MvStruct -> {
                        addAll(refItem.namedFields.asEntries())
                    }
                }
            }

            is MvSchema -> {
                addAll(scope.fieldsAsBindings.asEntries())
            }

            is MvModuleSpecBlock -> {
                val specFuns = scope.specFunctionList
                addAll(specFuns.asEntries())

                val specInlineFuns = scope.moduleItemSpecList.flatMap { it.specInlineFunctions() }
                addAll(specInlineFuns.asEntries())

                addAll(scope.schemaList.asEntries())
            }

            is MvCodeBlock -> {
                val (letBindings, _) = getVisibleLetPatBindingsWithShadowing(scope, cameFrom, ctx)
                addAll(letBindings.asEntries())
            }

            is MvSpecCodeBlock -> {
                val (letBindings, visited) = getVisibleLetPatBindingsWithShadowing(scope, cameFrom, ctx)
                addAll(letBindings.asEntries())

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
