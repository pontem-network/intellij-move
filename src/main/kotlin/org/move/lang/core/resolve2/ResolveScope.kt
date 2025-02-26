package org.move.lang.core.resolve2

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.ext.item
import org.move.lang.core.resolve.ScopeEntry
import org.move.lang.core.resolve.ScopeEntryWithVisibility
import org.move.lang.core.resolve.asEntry
import org.move.lang.core.resolve.ref.NAMES

val MvModule.itemEntries: List<ScopeEntryWithVisibility>
    get() {
        return listOf(
            // consts
            this.constList.mapNotNull { it.asEntry() },

            // types
            this.enumList.mapNotNull { it.asEntry() },
            this.schemaList.mapNotNull { it.asEntry() },
            this.structs().mapNotNull { it.asEntry() },

            // callables
            this.allNonTestFunctions().mapNotNull { it.asEntry() },
            this.tupleStructs().mapNotNull { it.asEntry()?.copyWithNs(NAMES) },

            // spec callables
            this.specFunctionList.mapNotNull { it.asEntry() },
            this.moduleItemSpecList.flatMap { it.specInlineFunctions() }.mapNotNull { it.asEntry() },
        ).flatten()
    }

object ResolveScopeUtil {
    fun scopeEntries(module: MvModule): List<ScopeEntry> {
        val itemEntries = module.itemEntries
        val entries = listOf(
            itemEntries,
            // variants
            module.enumVariants().mapNotNull { it.asEntry() },
            // builtins
            module.builtinFunctions().mapNotNull { it.asEntry() },
            module.builtinSpecFunctions().mapNotNull { it.asEntry() },
        ).flatten()
        return entries
    }

    fun scopeEntries(script: MvScript): List<ScopeEntry> {
        return script.constList.mapNotNull { it.asEntry() }
    }

    fun scopeEntries(functionLike: MvFunctionLike): List<ScopeEntry> {
        return functionLike.parametersAsBindings.map { it.asEntry() }
    }

    fun scopeEntries(lambda: MvLambdaExpr): List<ScopeEntry> {
        return lambda.lambdaParametersAsBindings.map { it.asEntry() }
    }

    fun scopeEntries(itemSpec: MvItemSpec): List<ScopeEntry> {
        val referencedItem = itemSpec.item
        return when (referencedItem) {
            is MvFunction -> {
                listOf(
                    referencedItem.typeParameters.mapNotNull { it.asEntry() },
                    referencedItem.parametersAsBindings.map { it.asEntry() },
                    referencedItem.specFunctionResultParameters.map { it.patBinding }.map { it.asEntry() },
                )
                    .flatten()
//                if (processor.processAll(referencedItem.typeParameters.mapNotNull { it.asEntry() })) return true
//
//                if (processor.processAll(
//                        referencedItem.parametersAsBindings.map { it.asEntry() },
//                        referencedItem.specFunctionResultParameters.map { it.patBinding }.map { it.asEntry() }
//                    )
//                ) {
//                    return true
//                }
            }
            is MvStruct -> {
                referencedItem.namedFields.mapNotNull { it.asEntry() }
//                if (processor.processAll(referencedItem.namedFields.mapNotNull { it.asEntry() })) return true
            }
            else -> emptyList()
        }
    }
}