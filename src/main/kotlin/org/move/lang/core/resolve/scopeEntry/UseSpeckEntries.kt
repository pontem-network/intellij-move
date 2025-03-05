package org.move.lang.core.resolve.scopeEntry

import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.ext.MvItemsOwner
import org.move.lang.core.resolve.ref.MODULES
import org.move.lang.index.MvItemNamespaceIndex
import org.move.lang.moveProject
import org.move.utils.PsiCachedValueProvider
import org.move.utils.getResults
import org.move.utils.psiCacheResult

val MvItemsOwner.useSpeckEntries: List<ScopeEntry> get() = UseSpeckEntries(this).getResults()

class UseSpeckEntries(override val owner: MvItemsOwner): PsiCachedValueProvider<List<ScopeEntry>> {
    override fun compute(): CachedValueProvider.Result<List<ScopeEntry>> {
        return owner.psiCacheResult(
            owner.useSpeckEntries()
        )
    }
}

private fun MvItemsOwner.useSpeckEntries(): List<ScopeEntry> {
    val searchScope = this.moveProject?.searchScope() ?: return emptyList()
    val useItems = this.useStmtList.useItems
    return buildList(useItems.size) {
        for (useItem in useItems) {
            val itemNs = when (useItem.type) {
                is UseItemType2.Module, is UseItemType2.SelfModule -> MODULES
                is UseItemType2.Item -> {
                    MvItemNamespaceIndex.getItemNs(searchScope, useItem.type.fqName)
                }
            }
            add(
                ScopeEntry(
                    useItem.nameOrAlias,
                    lazy { useItem.alias ?: useItem.useSpeck.path.reference?.resolve() },
                    itemNs,
                    customItemScope = useItem.scope,
                )
            )
        }
    }
}
