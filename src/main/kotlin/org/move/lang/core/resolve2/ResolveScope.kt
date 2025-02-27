package org.move.lang.core.resolve2

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ScopeEntry
import org.move.lang.core.resolve.asEntries
import org.move.lang.core.resolve.asEntry
import org.move.lang.core.resolve.ref.NAMES

val MvModule.itemEntries: List<ScopeEntry>
    get() {
        return listOf(
            // consts
            this.constList.asEntries(),

            // types
            this.enumList.asEntries(),
            this.schemaList.asEntries(),
            this.structs().asEntries(),

            // callables
            this.allNonTestFunctions().asEntries(),
            this.tupleStructs().mapNotNull { it.asEntry()?.copyWithNs(NAMES) },

            // spec callables
            this.specFunctionList.asEntries(),
            this.moduleItemSpecList.flatMap { it.specInlineFunctions() }.asEntries(),
        ).flatten()
    }

object ResolveScopeUtil {
    fun scopeEntries(module: MvModule): List<ScopeEntry> {
        val itemEntries = module.itemEntries
        val entries = listOf(
            itemEntries,
            // variants
            module.enumVariants().asEntries(),
            // builtins
            module.builtinFunctions().asEntries(),
            module.builtinSpecFunctions().asEntries(),
        ).flatten()
        return entries
    }

    fun scopeEntries(script: MvScript): List<ScopeEntry> {
        return script.constList.asEntries()
    }

    fun scopeEntries(functionLike: MvFunctionLike): List<ScopeEntry> {
        return functionLike.parametersAsBindings.asEntries()
    }

    fun scopeEntries(lambda: MvLambdaExpr): List<ScopeEntry> {
        return lambda.lambdaParametersAsBindings.asEntries()
    }

    fun scopeEntries(itemSpec: MvItemSpec): List<ScopeEntry> {
        val referencedItem = itemSpec.item
        return when (referencedItem) {
            is MvFunction -> {
                listOf(
                    referencedItem.typeParameters.asEntries(),
                    referencedItem.parametersAsBindings.asEntries(),
                    referencedItem.specFunctionResultParameters.map { it.patBinding }.asEntries(),
                )
                    .flatten()
            }
            is MvStruct -> {
                referencedItem.namedFields.asEntries()
            }
            else -> emptyList()
        }
    }
}