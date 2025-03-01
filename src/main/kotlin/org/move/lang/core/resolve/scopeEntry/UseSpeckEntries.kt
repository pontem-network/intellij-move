package org.move.lang.core.resolve.scopeEntry

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import org.move.ide.inspections.imports.usageScope
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvUseAlias
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.psi.ext.MvItemsOwner
import org.move.lang.core.psi.ext.qualifier
import org.move.lang.core.resolve.ref.ALL_NS
import org.move.lang.core.resolve.ref.itemNs
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.psiCacheResult

private val USE_SPECK_ENTRIES: Key<CachedValue<List<ScopeEntry>>> = Key.create("USE_SPECK_ENTRIES")

val MvItemsOwner.useSpeckEntries: List<ScopeEntry>
    get() {
        val stmts = this.useStmtList
        if (stmts.isEmpty()) return emptyList()
        return project.cacheManager.cache(this, USE_SPECK_ENTRIES) {
            psiCacheResult(this.useStmtList.useSpeckEntries())
        }
    }

private fun List<MvUseStmt>.useSpeckEntries(): List<ScopeEntry> {
    val speckItems = this.getUseSpeckItems()
    return buildList {
        for (speckItem in speckItems) {
            val speckPath = speckItem.speckPath
            val referencedItem = speckPath.reference?.resolve()

            val alias = speckItem.alias
            if (referencedItem == null) {
                // aliased element cannot be resolved, but alias itself is valid, resolve to it
                if (alias != null) {
                    // any ns is possible here
                    add(
                        ScopeEntry(
                            speckItem.speckName,
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
                    speckItem.speckName,
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
