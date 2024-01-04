package org.move.lang.core.resolve

import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace

fun processItemsInScope(
    scope: MvElement,
    cameFrom: MvElement,
    namespaces: Set<Namespace>,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (namespace in namespaces) {
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
                                when (itemVis.letStmtScope) {
                                    LetStmtScope.EXPR_STMT -> scope.allLetStmts
                                    LetStmtScope.LET_STMT, LetStmtScope.LET_POST_STMT -> {
                                        val letDecls = if (itemVis.letStmtScope == LetStmtScope.LET_POST_STMT) {
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
