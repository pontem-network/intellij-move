package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace

fun processItemsInScope(
    scope: MvElement,
    cameFrom: MvElement,
    namespaces: Set<Namespace>,
    contextScopeInfo: ContextScopeInfo,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (namespace in namespaces) {
        val stop = when (namespace) {

            Namespace.CONST -> {
                val found = when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.matchAll(
                            contextScopeInfo,
                            module.consts(),
                        )
                    }
                    else -> false
                }
                if (!found) {
                    if (scope is MvImportsOwner) {
                        if (processor.matchAll(contextScopeInfo, scope.allUseItems())) return true
                    }
                }
                found
            }

            Namespace.NAME -> {
                val found = when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.matchAll(
                            contextScopeInfo,
                            module.structs(),
                            module.consts(),
                        )
                    }
                    is MvModuleSpecBlock -> processor.matchAll(contextScopeInfo, scope.schemaList)
                    is MvScript -> processor.matchAll(contextScopeInfo, scope.consts())
                    is MvFunctionLike -> processor.matchAll(contextScopeInfo, scope.allParamsAsBindings)
                    is MvLambdaExpr -> processor.matchAll(contextScopeInfo, scope.bindingPatList)
                    is MvItemSpec -> {
                        val item = scope.item
                        when (item) {
                            is MvFunction -> {
                                processor.matchAll(
                                    contextScopeInfo,
                                    item.valueParamsAsBindings,
                                    item.specResultParameters.map { it.bindingPat },
                                )
                            }
                            is MvStruct -> processor.matchAll(contextScopeInfo, item.fields)
                            else -> false
                        }
                    }
                    is MvSchema -> processor.matchAll(contextScopeInfo, scope.fieldBindings)
                    is MvQuantBindingsOwner -> processor.matchAll(contextScopeInfo, scope.bindings)
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
                                when (contextScopeInfo.letStmtScope) {
                                    LetStmtScope.EXPR_STMT -> scope.allLetStmts
                                    LetStmtScope.LET_STMT, LetStmtScope.LET_POST_STMT -> {
                                        val letDecls = if (contextScopeInfo.letStmtScope == LetStmtScope.LET_POST_STMT) {
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
                                    LetStmtScope.NONE -> emptyList()
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
                        var found = processorWithShadowing.matchAll(contextScopeInfo, namedElements)
                        if (!found && scope is MvSpecCodeBlock) {
                            // if inside SpecCodeBlock, match also with builtin spec consts and global variables
                            found = processorWithShadowing.matchAll(
                                contextScopeInfo,
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
                        if (processor.matchAll(contextScopeInfo, scope.allUseItems())) return true
                    }
                }
                found
            }
            Namespace.FUNCTION -> {
                val found = when (scope) {
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        val specFunctions = if (contextScopeInfo.isMslScope) {
                            listOf(module.specFunctions(), module.builtinSpecFunctions()).flatten()
                        } else {
                            emptyList()
                        }
                        val specInlineFunctions = if (contextScopeInfo.isMslScope) {
                            module.moduleItemSpecs().flatMap { it.specInlineFunctions() }
                        } else {
                            emptyList()
                        }
                        processor.matchAll(
                            contextScopeInfo,
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
                            contextScopeInfo,
                            specFunctions,
                            specInlineFunctions
                        )
                    }
                    is MvFunctionLike -> processor.matchAll(contextScopeInfo, scope.lambdaParamsAsBindings)
                    is MvLambdaExpr -> processor.matchAll(contextScopeInfo, scope.bindingPatList)
                    is MvItemSpec -> {
                        val item = scope.item
                        when (item) {
                            is MvFunction -> processor.matchAll(contextScopeInfo, item.lambdaParamsAsBindings)
                            else -> false
                        }
                    }
                    is MvSpecCodeBlock -> {
                        val inlineFunctions = scope.specInlineFunctions().asReversed()
                        return processor.matchAll(contextScopeInfo, inlineFunctions)
                    }
                    else -> false
                }
                if (!found) {
                    if (scope is MvImportsOwner) {
                        if (processor.matchAll(contextScopeInfo, scope.allUseItems())) return true
                    }
                }
                found
            }

            Namespace.TYPE -> {
                if (scope is MvTypeParametersOwner) {
                    if (processor.matchAll(contextScopeInfo, scope.typeParameters)) return true
                }
                val found = when (scope) {
                    is MvItemSpec -> {
                        val funcItem = scope.funcItem
                        if (funcItem != null) {
                            processor.matchAll(contextScopeInfo, funcItem.typeParameters)
                        } else {
                            false
                        }
                    }
                    is MvModuleBlock -> {
                        val module = scope.parent as MvModule
                        processor.matchAll(
                            contextScopeInfo,
                            scope.allUseItems(),
                            module.structs()
                        )
                    }
                    is MvApplySchemaStmt -> {
                        val toPatterns = scope.applyTo?.functionPatternList.orEmpty()
                        val patternTypeParams =
                            toPatterns.flatMap { it.typeParameterList?.typeParameterList.orEmpty() }
                        processor.matchAll(contextScopeInfo, patternTypeParams)
                    }

                    else -> false
                }
                if (!found) {
                    if (scope is MvImportsOwner) {
                        if (processor.matchAll(contextScopeInfo, scope.allUseItems())) return true
                    }
                }
                found
            }

            Namespace.SCHEMA -> when (scope) {
                is MvModuleBlock -> processor.matchAll(
                    contextScopeInfo,
                    scope.allUseItems(),
                    scope.schemaList
                )
                is MvModuleSpecBlock -> processor.matchAll(
                    contextScopeInfo,
                    scope.allUseItems(),
                    scope.schemaList,
                    scope.specFunctionList
                )
                else -> false
            }

            Namespace.MODULE -> when (scope) {
                is MvImportsOwner ->
                    processor.matchAll(contextScopeInfo, scope.moduleUseItems())
                else -> false
            }
        }
        if (stop) return true
    }
    return false
}
