package org.move.lang.core.resolve.scopeEntry

import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.allNonTestFunctions
import org.move.lang.core.psi.ext.specInlineFunctions
import org.move.lang.core.psi.ext.tupleStructs
import org.move.lang.core.resolve.ref.NAMES

val MvModule.itemEntries: List<ScopeEntry>
    get() {
        return getProjectPsiDependentCache(this, MvModule::itemEntriesInner)
    }

private val MvModule.itemEntriesInner: List<ScopeEntry> get() {
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
