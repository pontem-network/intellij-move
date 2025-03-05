package org.move.lang.core.resolve.scopeEntry

import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.allNonTestFunctions
import org.move.lang.core.psi.ext.specInlineFunctions
import org.move.lang.core.psi.ext.tupleStructs
import org.move.utils.PsiCachedValueProvider
import org.move.utils.getResults
import org.move.lang.core.resolve.ref.NAMES
import org.move.utils.psiCacheResult

val MvModule.itemEntries: List<ScopeEntry>
    get() {
        return getItemEntriesInner(this)
    }

class ItemEntries(override val owner: MvModule): PsiCachedValueProvider<List<ScopeEntry>> {
    override fun compute(): CachedValueProvider.Result<List<ScopeEntry>> {
        val entries =
            buildList(owner.children.size / 2) {
                // consts
                addAll(owner.constList.asEntries())

                // types
                addAll(owner.enumList.asEntries())
                addAll(owner.schemaList.asEntries())
                addAll(owner.structList.asEntries())

                // callables
                addAll(owner.allNonTestFunctions().asEntries())
                addAll(owner.tupleStructs().mapNotNull { it.asEntry()?.copyWithNs(NAMES) })

                // spec callables
                addAll(owner.specFunctionList.asEntries())
                addAll(owner.moduleItemSpecList.flatMap { it.specInlineFunctions() }.asEntries())
            }
        // one hop up to get to the file, cheap enough to use
        return owner.psiCacheResult(entries)
    }
}

fun getItemEntriesInner(owner: MvModule): List<ScopeEntry> {
    val entries =
        buildList(owner.children.size / 2) {
            // consts
            addAll(owner.constList.asEntries())

            // types
            addAll(owner.enumList.asEntries())
            addAll(owner.schemaList.asEntries())
            addAll(owner.structList.asEntries())

            // callables
            addAll(owner.allNonTestFunctions().asEntries())
            addAll(owner.tupleStructs().mapNotNull { it.asEntry()?.copyWithNs(NAMES) })

            // spec callables
            addAll(owner.specFunctionList.asEntries())
            addAll(owner.moduleItemSpecList.flatMap { it.specInlineFunctions() }.asEntries())
        }
    return entries
}
