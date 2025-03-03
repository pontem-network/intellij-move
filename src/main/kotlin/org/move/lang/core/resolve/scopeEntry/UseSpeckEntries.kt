package org.move.lang.core.resolve.scopeEntry

import com.intellij.psi.util.CachedValueProvider
import org.move.ide.inspections.imports.usageScope
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvUseAlias
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.psi.ext.MvItemsOwner
import org.move.lang.core.psi.ext.qualifier
import org.move.utils.PsiCachedValueProvider
import org.move.utils.getResults
import org.move.lang.core.resolve.ref.ALL_NS
import org.move.lang.core.resolve.ref.itemNs
import org.move.utils.psiCacheResult

val MvItemsOwner.useSpeckEntries: List<ScopeEntry> get() = UseSpeckEntries(this).getResults()

class UseSpeckEntries(override val owner: MvItemsOwner): PsiCachedValueProvider<List<ScopeEntry>> {
    override fun compute(): CachedValueProvider.Result<List<ScopeEntry>> {
        return owner.psiCacheResult(
            owner.useStmtList.useSpeckEntries()
        )
    }
}

private fun List<MvUseStmt>.useSpeckEntries(): List<ScopeEntry> {
    val speckItems = this.getUseSpeckItems()
    return buildList {
        for (speckItem in speckItems) {
            val speckPath = speckItem.speckPath
            val itemName = speckItem.speckName
            val referencedItem = speckPath.reference?.resolve()

            val alias = speckItem.alias
            if (referencedItem == null) {
                // aliased element cannot be resolved, but alias itself is valid, resolve to it
                if (alias != null) {
                    // any ns is possible here
                    add(
                        ScopeEntry(
                            itemName,
                            alias,
                            ALL_NS
                        )
                    )
                }
                continue
            }
            val element = alias ?: referencedItem
            val itemNs = referencedItem.itemNs
            add(
                ScopeEntry(
                    itemName,
                    element,
                    itemNs,
                    customItemScope = speckItem.speckUsageScope,
                )
            )
        }
    }
}


private fun List<MvUseStmt>.getUseSpeckItems(): List<UseSpeckItem> {
    val useStmts = this
    return buildList {
        for (useStmt in useStmts) {
            val stmtSpecks = useStmt.useSpeckLeaves
            val usageScope = useStmt.usageScope
            for ((speckPath, alias) in stmtSpecks) {
                val speckName = getUseSpeckName(speckPath, alias)
                if (speckName != null) {
                    add(UseSpeckItem(speckPath, speckName, usageScope, alias))
                }
            }
        }
    }
}

private data class UseSpeckItem(
    val speckPath: MvPath,
    val speckName: String,
    val speckUsageScope: NamedItemScope,
    val alias: MvUseAlias?,
)

private fun getUseSpeckName(path: MvPath, alias: MvUseAlias?): String? {
    return if (alias != null) {
        alias.name
    } else {
        var n = path.referenceName ?: return null
        // 0x1::m::Self -> 0x1::m
        if (n == "Self") {
            n = path.qualifier?.referenceName ?: return null
        }
        n
    }
}

val MvUseStmt.useSpeckLeaves: List<UseSpeckLeaf>
    get() {
        val useStmt = this
        return buildList {
            val rootUseSpeck = useStmt.useSpeck ?: return@buildList
            val useGroup = rootUseSpeck.useGroup
            if (useGroup == null) {
                // basePath is null, path is full path of useSpeck
                val alias = rootUseSpeck.useAlias
                add(UseSpeckLeaf(rootUseSpeck.path, alias))
            } else {
                for (childSpeck in useGroup.useSpeckList) {
                    val childAlias = childSpeck.useAlias
                    add(UseSpeckLeaf(childSpeck.path, childAlias))
                }
            }
        }
    }

data class UseSpeckLeaf(val usePath: MvPath, val useAlias: MvUseAlias?)
