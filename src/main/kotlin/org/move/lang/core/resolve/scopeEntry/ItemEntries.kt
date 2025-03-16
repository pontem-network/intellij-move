package org.move.lang.core.resolve.scopeEntry

import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.allNonTestFunctions
import org.move.lang.core.psi.ext.specInlineFunctions
import org.move.lang.core.psi.ext.tupleStructs
import org.move.lang.core.resolve.ref.Ns

val MvModule.itemEntries: List<ScopeEntry>
    get() {
        return getItemEntriesInner(this)
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
            addAll(owner.tupleStructs().mapNotNull { it.asEntry()?.copyWithNs(Ns.NAME) })

            // spec callables
            addAll(owner.specFunctionList.asEntries())
            addAll(owner.moduleItemSpecList.flatMap { it.specInlineFunctions() }.asEntries())
        }
    return entries
}
