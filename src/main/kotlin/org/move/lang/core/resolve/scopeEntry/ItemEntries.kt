package org.move.lang.core.resolve.scopeEntry

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.allNonTestFunctions
import org.move.lang.core.psi.ext.specInlineFunctions
import org.move.lang.core.psi.ext.tupleStructs
import org.move.lang.core.resolve.ref.NAMES
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.psiCacheResult

private val ITEM_ENTRIES_KEY: Key<CachedValue<List<ScopeEntry>>> = Key.create("ITEM_ENTRIES_KEY")

val MvModule.itemEntries: List<ScopeEntry>
    get() {
        return project.cacheManager
            .cache(this, ITEM_ENTRIES_KEY) {
                this.psiCacheResult(this.itemEntriesInner)
            }
    }

private val MvModule.itemEntriesInner: List<ScopeEntry>
    get() {
        return listOf(
            // consts
            this.constList.asEntries(),

            // types
            this.enumList.asEntries(),
            this.schemaList.asEntries(),
            this.structList.asEntries(),

            // callables
            this.allNonTestFunctions().asEntries(),
            this.tupleStructs().mapNotNull { it.asEntry()?.copyWithNs(NAMES) },

            // spec callables
            this.specFunctionList.asEntries(),
            this.moduleItemSpecList.flatMap { it.specInlineFunctions() }.asEntries(),
        ).flatten()
    }
