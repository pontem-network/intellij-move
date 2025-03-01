package org.move.lang.core.resolve

import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvModuleSpecBlock
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.builtinFunctions
import org.move.lang.core.psi.builtinSpecFunctions
import org.move.lang.core.psi.ext.enumVariants
import org.move.lang.core.psi.ext.item
import org.move.lang.core.psi.ext.namedFields
import org.move.lang.core.psi.ext.specFunctionResultParameters
import org.move.lang.core.psi.ext.specInlineFunctions
import org.move.lang.core.psi.parametersAsBindings
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.scopeEntry.asEntries
import org.move.lang.core.resolve.scopeEntry.itemEntries
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.psiCacheResult

interface PsiCachedValueProvider<T>: CachedValueProvider<T> {
    val owner: MvElement

    abstract override fun compute(): CachedValueProvider.Result<T>
}

fun <T> PsiCachedValueProvider<T>.getResults(): T {
    val manager = this.owner.project.cacheManager
    val key = manager.getKeyForClass<T>(this::class.java)
    return manager.cache(owner, key, this)
}

class ModuleResolveScope(override val owner: MvModule): PsiCachedValueProvider<List<ScopeEntry>> {
    override fun compute(): CachedValueProvider.Result<List<ScopeEntry>> {
        val entries = buildList {
            addAll(owner.itemEntries)
            addAll(owner.enumVariants().asEntries())
            addAll(owner.builtinFunctions().asEntries())
            addAll(owner.builtinSpecFunctions().asEntries())
        }
        return owner.psiCacheResult(entries)
    }
}

class ItemSpecResolveScope(override val owner: MvItemSpec): PsiCachedValueProvider<List<ScopeEntry>> {
    override fun compute(): CachedValueProvider.Result<List<ScopeEntry>> {
        val entries = buildList {
            val refItem = owner.item
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
        return owner.psiCacheResult(entries)
    }
}

class ModuleSpecBlockResolveScope(override val owner: MvModuleSpecBlock): PsiCachedValueProvider<List<ScopeEntry>> {
    override fun compute(): CachedValueProvider.Result<List<ScopeEntry>> {
        val entries = buildList {
            val specFuns = owner.specFunctionList
            addAll(specFuns.asEntries())

            val specInlineFuns = owner.moduleItemSpecList.flatMap { it.specInlineFunctions() }
            addAll(specInlineFuns.asEntries())

            addAll(owner.schemaList.asEntries())
        }
        return owner.psiCacheResult(entries)
    }
}
